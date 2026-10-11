package com.ooad.cosmetics.integration;

import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.dashboard.DashboardSummaryResponse;
import com.ooad.cosmetics.dto.dashboard.RevenueGrouping;
import com.ooad.cosmetics.dto.dashboard.RevenuePointResponse;
import com.ooad.cosmetics.dto.dashboard.TopProductResponse;
import com.ooad.cosmetics.dto.order.CreateOrderRequest;
import com.ooad.cosmetics.dto.order.OrderResponse;
import com.ooad.cosmetics.dto.order.OrderSummaryResponse;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.entity.PaymentMethod;
import com.ooad.cosmetics.service.CheckoutService;
import com.ooad.cosmetics.service.DashboardService;
import com.ooad.cosmetics.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DB14: các truy vấn thống kê của Dashboard và bộ lọc đơn của Admin, chạy trên PostgreSQL thật.
 *
 * Dữ liệu: 4 khách đặt đơn
 * - c1: 2 x sản phẩm A (230.000 gồm ship) -> DELIVERED
 * - c2: 4 x sản phẩm B (230.000 gồm ship) -> DELIVERED
 * - c3: 1 x sản phẩm A (130.000)          -> CANCELLED
 * - c4: 1 x sản phẩm B (80.000)           -> PENDING
 */
@Tag("integration")
class DashboardIntegrationTest extends IntegrationTestBase {

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private DashboardService dashboardService;

    private long variantA;
    private long variantB;
    private OrderResponse orderOfC1;

    @BeforeEach
    void seedOrders() {
        createAdmin("admin@example.com");
        variantA = createVariant("100000.00", 20);
        variantB = createVariant("50000.00", 20);

        orderOfC1 = place("c1@example.com", variantA, 2);
        deliver(orderOfC1);

        deliver(place("c2@example.com", variantB, 4));

        place("c3@example.com", variantA, 1);
        orderService.cancelMine(lastOrderId());

        place("c4@example.com", variantB, 1);
    }

    private OrderResponse place(String email, long variantId, int quantity) {
        long addressId = prepareCustomerWithCart(email, variantId, quantity);
        return checkoutService.createOrder(
                new CreateOrderRequest(addressId, null, PaymentMethod.COD, null)
        );
    }

    private void deliver(OrderResponse order) {
        orderService.updateStatus(order.id(), OrderStatus.CONFIRMED);
        orderService.updateStatus(order.id(), OrderStatus.SHIPPING);
        orderService.updateStatus(order.id(), OrderStatus.DELIVERED);
    }

    private long lastOrderId() {
        return jdbc.queryForObject("SELECT MAX(id) FROM orders", Long.class);
    }

    // ---------------- summary ----------------

    @Test
    void summaryCountsEverythingAndRevenueOnlyCountsDeliveredOrders() {
        DashboardSummaryResponse summary = dashboardService.summary(10);

        assertThat(summary.totalCustomers()).isEqualTo(4);   // admin không được tính
        assertThat(summary.totalProducts()).isEqualTo(2);
        assertThat(summary.totalOrders()).isEqualTo(4);
        assertThat(summary.totalRevenue()).isEqualByComparingTo(new BigDecimal("460000"));

        assertThat(summary.ordersByStatus().get(OrderStatus.DELIVERED)).isEqualTo(2L);
        assertThat(summary.ordersByStatus().get(OrderStatus.CANCELLED)).isEqualTo(1L);
        assertThat(summary.ordersByStatus().get(OrderStatus.PENDING)).isEqualTo(1L);
        assertThat(summary.ordersByStatus().get(OrderStatus.CONFIRMED)).isZero();
        assertThat(summary.ordersByStatus().get(OrderStatus.SHIPPING)).isZero();
    }

    @Test
    void lowStockListUsesTheThresholdAndOnlyActiveVariants() {
        // Tồn kho hiện tại: A = 18 (đơn bị hủy đã hoàn kho), B = 15.
        assertThat(stockOf(variantA)).isEqualTo(18);
        assertThat(stockOf(variantB)).isEqualTo(15);

        assertThat(dashboardService.summary(14).lowStockVariants()).isEmpty();

        DashboardSummaryResponse at15 = dashboardService.summary(15);
        assertThat(at15.lowStockVariants()).hasSize(1);
        assertThat(at15.lowStockVariants().get(0).id()).isEqualTo(variantB);

        jdbc.update("UPDATE product_variants SET status = 'INACTIVE' WHERE id = ?", variantB);
        assertThat(dashboardService.summary(15).lowStockVariants()).isEmpty();
    }

    // ---------------- revenue ----------------

    @Test
    void revenueByDayPutsDeliveredOrdersOnTodayAndZeroOnOtherDays() {
        LocalDate today = LocalDate.now(VIETNAM);

        List<RevenuePointResponse> points =
                dashboardService.revenue(today.minusDays(1), today, RevenueGrouping.DAY);

        assertThat(points).hasSize(2);
        assertThat(points.get(0).period()).isEqualTo(today.minusDays(1).toString());
        assertThat(points.get(0).revenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(points.get(1).period()).isEqualTo(today.toString());
        assertThat(points.get(1).revenue()).isEqualByComparingTo(new BigDecimal("460000"));
        assertThat(points.get(1).orderCount()).isEqualTo(2L);
    }

    @Test
    void revenueByMonthGroupsTheSameOrdersIntoOnePoint() {
        LocalDate today = LocalDate.now(VIETNAM);

        List<RevenuePointResponse> points =
                dashboardService.revenue(today.withDayOfMonth(1), today, RevenueGrouping.MONTH);

        assertThat(points).hasSize(1);
        assertThat(points.get(0).revenue()).isEqualByComparingTo(new BigDecimal("460000"));
    }

    // ---------------- top products ----------------

    @Test
    void topProductsAreRankedByQuantityAndIgnoreCancelledAndPendingOrders() {
        List<TopProductResponse> top = dashboardService.topProducts(5);

        assertThat(top).hasSize(2);
        assertThat(top.get(0).productId()).isEqualTo(productIdOf(variantB));
        assertThat(top.get(0).quantitySold()).isEqualTo(4L);
        assertThat(top.get(0).revenue()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(top.get(1).productId()).isEqualTo(productIdOf(variantA));
        assertThat(top.get(1).quantitySold()).isEqualTo(2L);

        assertThat(dashboardService.topProducts(1)).hasSize(1);
    }

    // ---------------- admin order list (O25) ----------------

    @Test
    void adminCanFilterOrdersByStatusKeywordAndDateRange() {
        LocalDate today = LocalDate.now(VIETNAM);

        PageResponse<OrderSummaryResponse> delivered =
                orderService.adminList(OrderStatus.DELIVERED, null, null, null, 0, 20);
        assertThat(delivered.totalElements()).isEqualTo(2);

        PageResponse<OrderSummaryResponse> byEmail =
                orderService.adminList(null, "c3@example", null, null, 0, 20);
        assertThat(byEmail.totalElements()).isEqualTo(1);
        assertThat(byEmail.content().get(0).customerEmail()).isEqualTo("c3@example.com");
        assertThat(byEmail.content().get(0).status()).isEqualTo(OrderStatus.CANCELLED);

        PageResponse<OrderSummaryResponse> byId =
                orderService.adminList(null, String.valueOf(orderOfC1.id()), null, null, 0, 20);
        assertThat(byId.content()).extracting(OrderSummaryResponse::id).contains(orderOfC1.id());

        assertThat(orderService.adminList(null, null, today, today, 0, 20).totalElements()).isEqualTo(4);
        assertThat(orderService.adminList(null, null, today.plusDays(1), null, 0, 20).totalElements()).isZero();

        PageResponse<OrderSummaryResponse> firstPage = orderService.adminList(null, null, null, null, 0, 3);
        assertThat(firstPage.content()).hasSize(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        // Mới nhất đứng đầu.
        assertThat(firstPage.content().get(0).id()).isGreaterThan(firstPage.content().get(1).id());
    }

    @Test
    void summaryItemsExposeItemCountPaymentAndFirstProduct() {
        PageResponse<OrderSummaryResponse> page =
                orderService.adminList(OrderStatus.DELIVERED, "c2@example", null, null, 0, 20);

        OrderSummaryResponse row = page.content().get(0);
        assertThat(row.itemCount()).isEqualTo(4);
        assertThat(row.firstItemName()).isNotBlank();
        assertThat(row.paymentMethod()).isEqualTo(PaymentMethod.COD);
        assertThat(row.paymentStatus().name()).isEqualTo("PAID");
        assertThat(row.totalAmount()).isEqualByComparingTo(new BigDecimal("230000"));
    }
}
