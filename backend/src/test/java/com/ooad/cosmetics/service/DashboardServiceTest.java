package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.dto.dashboard.DashboardSummaryResponse;
import com.ooad.cosmetics.dto.dashboard.RevenueGrouping;
import com.ooad.cosmetics.dto.dashboard.RevenuePointResponse;
import com.ooad.cosmetics.dto.dashboard.TopProductResponse;
import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.entity.Product;
import com.ooad.cosmetics.entity.ProductVariant;
import com.ooad.cosmetics.entity.UserRole;
import com.ooad.cosmetics.repository.DashboardRepository;
import com.ooad.cosmetics.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private DashboardRepository dashboardRepository;
    private ProductVariantRepository variantRepository;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        dashboardRepository = mock(DashboardRepository.class);
        variantRepository = mock(ProductVariantRepository.class);
        service = new DashboardService(dashboardRepository, variantRepository);
    }

    // ---------------- summary ----------------

    @Test
    void summaryFillsMissingStatusesWithZeroAndTreatsNullRevenueAsZero() {
        when(dashboardRepository.countUsersByRole(UserRole.CUSTOMER)).thenReturn(5L);
        when(dashboardRepository.countProducts()).thenReturn(20L);
        when(dashboardRepository.countOrders()).thenReturn(7L);
        when(dashboardRepository.sumRevenueByStatus(OrderStatus.DELIVERED)).thenReturn(null);
        when(dashboardRepository.countOrdersGroupedByStatus()).thenReturn(List.of(
                new Object[]{OrderStatus.PENDING, 3L},
                new Object[]{OrderStatus.DELIVERED, 4L}
        ));
        when(variantRepository.findByStatusAndStockLessThanEqual(
                eq(CatalogStatus.ACTIVE), eq(10), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(variant(10L, 2))));

        DashboardSummaryResponse summary = service.summary(10);

        assertThat(summary.totalCustomers()).isEqualTo(5L);
        assertThat(summary.totalProducts()).isEqualTo(20L);
        assertThat(summary.totalOrders()).isEqualTo(7L);
        assertThat(summary.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.ordersByStatus()).hasSize(OrderStatus.values().length);
        assertThat(summary.ordersByStatus().get(OrderStatus.PENDING)).isEqualTo(3L);
        assertThat(summary.ordersByStatus().get(OrderStatus.DELIVERED)).isEqualTo(4L);
        assertThat(summary.ordersByStatus().get(OrderStatus.CANCELLED)).isZero();
        assertThat(summary.lowStockThreshold()).isEqualTo(10);
        assertThat(summary.lowStockVariants()).hasSize(1);
        assertThat(summary.lowStockVariants().get(0).stock()).isEqualTo(2);
    }

    @Test
    void summaryRevenueComesOnlyFromDeliveredOrders() {
        when(dashboardRepository.sumRevenueByStatus(OrderStatus.DELIVERED))
                .thenReturn(new BigDecimal("1500000.00"));
        when(dashboardRepository.countOrdersGroupedByStatus()).thenReturn(List.of());
        when(variantRepository.findByStatusAndStockLessThanEqual(
                eq(CatalogStatus.ACTIVE), eq(5), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        DashboardSummaryResponse summary = service.summary(5);

        assertThat(summary.totalRevenue()).isEqualByComparingTo(new BigDecimal("1500000"));
        verify(dashboardRepository).sumRevenueByStatus(OrderStatus.DELIVERED);
    }

    // ---------------- revenue ----------------

    @Test
    void revenueGroupsByVietnamDayAndFillsDaysWithoutOrders() {
        when(dashboardRepository.findRevenueRows(
                eq(OrderStatus.DELIVERED), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(
                        // 10:00 giờ VN ngày 01/10
                        new Object[]{Instant.parse("2026-10-01T03:00:00Z"), new BigDecimal("100000.00")},
                        // 17:00 giờ VN ngày 01/10
                        new Object[]{Instant.parse("2026-10-01T10:00:00Z"), new BigDecimal("50000.00")},
                        // 18:30 UTC = 01:30 giờ VN ngày 02/10
                        new Object[]{Instant.parse("2026-10-01T18:30:00Z"), new BigDecimal("30000.00")},
                        // 12:00 giờ VN ngày 03/10
                        new Object[]{Instant.parse("2026-10-03T05:00:00Z"), new BigDecimal("200000.00")}
                ));

        List<RevenuePointResponse> points = service.revenue(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 4),
                RevenueGrouping.DAY
        );

        assertThat(points).extracting(RevenuePointResponse::period)
                .containsExactly("2026-10-01", "2026-10-02", "2026-10-03", "2026-10-04");
        assertThat(points.get(0).revenue()).isEqualByComparingTo(new BigDecimal("150000"));
        assertThat(points.get(0).orderCount()).isEqualTo(2L);
        assertThat(points.get(1).revenue()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(points.get(1).orderCount()).isEqualTo(1L);
        assertThat(points.get(2).revenue()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(points.get(3).revenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(points.get(3).orderCount()).isZero();
    }

    @Test
    void revenueGroupsByMonth() {
        when(dashboardRepository.findRevenueRows(
                eq(OrderStatus.DELIVERED), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(
                        new Object[]{Instant.parse("2026-09-20T05:00:00Z"), new BigDecimal("100000.00")},
                        new Object[]{Instant.parse("2026-10-05T05:00:00Z"), new BigDecimal("40000.00")},
                        new Object[]{Instant.parse("2026-10-06T05:00:00Z"), new BigDecimal("60000.00")}
                ));

        List<RevenuePointResponse> points = service.revenue(
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 10, 20),
                RevenueGrouping.MONTH
        );

        assertThat(points).extracting(RevenuePointResponse::period)
                .containsExactly("2026-09", "2026-10");
        assertThat(points.get(0).revenue()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(points.get(1).revenue()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(points.get(1).orderCount()).isEqualTo(2L);
    }

    @Test
    void revenueRejectsFromDateAfterToDate() {
        assertThatThrownBy(() -> service.revenue(
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1), RevenueGrouping.DAY))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("from must not be after to");
    }

    @Test
    void revenueRejectsRangesLongerThanOneYear() {
        assertThatThrownBy(() -> service.revenue(
                LocalDate.of(2025, 1, 1), LocalDate.of(2026, 10, 1), RevenueGrouping.DAY))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Date range must not exceed 366 days");
    }

    // ---------------- top products ----------------

    @Test
    void topProductsMapsRowsWhateverNumericTypesTheDatabaseReturns() {
        when(dashboardRepository.findTopSellingProducts(2)).thenReturn(List.of(
                new Object[]{5L, "Serum", new BigDecimal("12"), new BigDecimal("1200000.00")},
                new Object[]{6, "Cream", 7L, 700000.0}
        ));

        List<TopProductResponse> top = service.topProducts(2);

        assertThat(top).hasSize(2);
        assertThat(top.get(0).productId()).isEqualTo(5L);
        assertThat(top.get(0).productName()).isEqualTo("Serum");
        assertThat(top.get(0).quantitySold()).isEqualTo(12L);
        assertThat(top.get(0).revenue()).isEqualByComparingTo(new BigDecimal("1200000"));
        assertThat(top.get(1).productId()).isEqualTo(6L);
        assertThat(top.get(1).quantitySold()).isEqualTo(7L);
        assertThat(top.get(1).revenue()).isEqualByComparingTo(new BigDecimal("700000"));
    }

    @Test
    void topProductsRejectsLimitOutsideAllowedRange() {
        assertThatThrownBy(() -> service.topProducts(0))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("limit must be between 1 and 50");
        assertThatThrownBy(() -> service.topProducts(51))
                .isInstanceOf(BadRequestException.class);
    }

    // ---------------- helpers ----------------

    private ProductVariant variant(Long id, int stock) {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", 5L);
        product.setName("Serum");

        ProductVariant variant = new ProductVariant();
        ReflectionTestUtils.setField(variant, "id", id);
        variant.setProduct(product);
        variant.setSku("SKU-" + id);
        variant.setPrice(new BigDecimal("100000.00"));
        variant.setStock(stock);
        return variant;
    }
}
