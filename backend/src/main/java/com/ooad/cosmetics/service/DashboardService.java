package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.dto.dashboard.DashboardSummaryResponse;
import com.ooad.cosmetics.dto.dashboard.RevenueGrouping;
import com.ooad.cosmetics.dto.dashboard.RevenuePointResponse;
import com.ooad.cosmetics.dto.dashboard.TopProductResponse;
import com.ooad.cosmetics.dto.product.ProductVariantResponse;
import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.entity.UserRole;
import com.ooad.cosmetics.repository.DashboardRepository;
import com.ooad.cosmetics.repository.ProductVariantRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    /** Ngày trong báo cáo doanh thu được hiểu theo giờ Việt Nam. */
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final int LOW_STOCK_LIST_SIZE = 10;
    private static final int MAX_RANGE_DAYS = 365;
    private static final int MAX_TOP_PRODUCTS = 50;
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final DashboardRepository dashboardRepository;
    private final ProductVariantRepository variantRepository;

    public DashboardService(
            DashboardRepository dashboardRepository,
            ProductVariantRepository variantRepository
    ) {
        this.dashboardRepository = dashboardRepository;
        this.variantRepository = variantRepository;
    }

    /** DB01 - DB06 */
    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(int lowStockThreshold) {
        // DB04: doanh thu chỉ tính các đơn đã giao thành công.
        BigDecimal revenue = dashboardRepository.sumRevenueByStatus(OrderStatus.DELIVERED);

        // DB05: trạng thái nào chưa có đơn thì vẫn trả về 0.
        Map<OrderStatus, Long> byStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            byStatus.put(status, 0L);
        }
        for (Object[] row : dashboardRepository.countOrdersGroupedByStatus()) {
            byStatus.put((OrderStatus) row[0], ((Number) row[1]).longValue());
        }

        // DB06
        List<ProductVariantResponse> lowStock = variantRepository
                .findByStatusAndStockLessThanEqual(
                        CatalogStatus.ACTIVE,
                        lowStockThreshold,
                        PageRequest.of(
                                0,
                                LOW_STOCK_LIST_SIZE,
                                Sort.by(Sort.Direction.ASC, "stock")
                                        .and(Sort.by(Sort.Direction.ASC, "id"))
                        )
                )
                .map(ProductVariantResponse::from)
                .getContent();

        return new DashboardSummaryResponse(
                dashboardRepository.countUsersByRole(UserRole.CUSTOMER),
                dashboardRepository.countProducts(),
                dashboardRepository.countOrders(),
                revenue == null ? ZERO : revenue,
                byStatus,
                lowStockThreshold,
                lowStock
        );
    }

    /**
     * DB10: doanh thu theo ngày hoặc tháng, chỉ tính đơn DELIVERED theo ngày giao.
     * Ngày/tháng không có đơn vẫn xuất hiện với doanh thu 0 để vẽ biểu đồ liên tục.
     * Mặc định là 30 ngày gần nhất.
     */
    @Transactional(readOnly = true)
    public List<RevenuePointResponse> revenue(
            LocalDate from,
            LocalDate to,
            RevenueGrouping grouping
    ) {
        LocalDate end = to != null ? to : LocalDate.now(BUSINESS_ZONE);
        LocalDate start = from != null ? from : end.minusDays(29);
        RevenueGrouping mode = grouping == null ? RevenueGrouping.DAY : grouping;

        if (start.isAfter(end)) {
            throw new BadRequestException("from must not be after to");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_RANGE_DAYS) {
            throw new BadRequestException("Date range must not exceed 366 days");
        }

        List<Object[]> rows = dashboardRepository.findRevenueRows(
                OrderStatus.DELIVERED,
                start.atStartOfDay(BUSINESS_ZONE).toInstant(),
                end.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant()
        );

        Map<String, BigDecimal> revenueByPeriod = new HashMap<>();
        Map<String, Long> countByPeriod = new HashMap<>();
        for (Object[] row : rows) {
            Instant deliveredAt = (Instant) row[0];
            BigDecimal amount = toBigDecimal(row[1]);
            String key = label(deliveredAt.atZone(BUSINESS_ZONE).toLocalDate(), mode);

            revenueByPeriod.merge(key, amount, BigDecimal::add);
            countByPeriod.merge(key, 1L, Long::sum);
        }

        List<RevenuePointResponse> points = new ArrayList<>();
        LocalDate cursor = mode == RevenueGrouping.DAY ? start : start.withDayOfMonth(1);
        LocalDate last = mode == RevenueGrouping.DAY ? end : end.withDayOfMonth(1);

        while (!cursor.isAfter(last)) {
            String key = label(cursor, mode);
            points.add(new RevenuePointResponse(
                    key,
                    revenueByPeriod.getOrDefault(key, ZERO),
                    countByPeriod.getOrDefault(key, 0L)
            ));
            cursor = mode == RevenueGrouping.DAY ? cursor.plusDays(1) : cursor.plusMonths(1);
        }

        return points;
    }

    /** DB11 */
    @Transactional(readOnly = true)
    public List<TopProductResponse> topProducts(int limit) {
        if (limit < 1 || limit > MAX_TOP_PRODUCTS) {
            throw new BadRequestException("limit must be between 1 and " + MAX_TOP_PRODUCTS);
        }

        return dashboardRepository.findTopSellingProducts(limit).stream()
                .map(row -> new TopProductResponse(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        toBigDecimal(row[3])
                ))
                .toList();
    }

    private String label(LocalDate date, RevenueGrouping mode) {
        return mode == RevenueGrouping.DAY ? date.toString() : YearMonth.from(date).toString();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return ZERO;
        }
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
    }
}
