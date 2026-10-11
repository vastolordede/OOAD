package com.ooad.cosmetics.integration;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.dto.cart.AddCartItemRequest;
import com.ooad.cosmetics.dto.order.CreateOrderRequest;
import com.ooad.cosmetics.dto.order.OrderResponse;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.entity.PaymentMethod;
import com.ooad.cosmetics.service.CheckoutService;
import com.ooad.cosmetics.service.OrderService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O13 - O18, O43 - O47: tạo đơn, hủy đơn, đổi trạng thái, rollback và đồng thời. */
@Tag("integration")
class CheckoutOrderIntegrationTest extends IntegrationTestBase {

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private OrderService orderService;

    private OrderResponse placeOrder(long addressId, String voucherCode) {
        return checkoutService.createOrder(
                new CreateOrderRequest(addressId, voucherCode, PaymentMethod.COD, null)
        );
    }

    // ------------------------------------------------------------------
    // Tạo đơn thành công
    // ------------------------------------------------------------------

    @Test
    void createOrderSavesEverythingDeductsStockAndClearsTheCart() {
        long variantId = createVariant("100000.00", 5);
        long addressId = prepareCustomerWithCart("buyer@example.com", variantId, 2);

        OrderResponse order = placeOrder(addressId, null);

        assertThat(order.totalAmount()).isEqualByComparingTo(new BigDecimal("230000"));
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);

        assertThat(stockOf(variantId)).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM orders")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM order_items")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM payments", String.class)).isEqualTo("PENDING");
        assertThat(count("SELECT COUNT(*) FROM cart_items")).isZero();

        // O16: snapshot tên sản phẩm, SKU và giá tại thời điểm đặt.
        assertThat(jdbc.queryForObject("SELECT unit_price FROM order_items", BigDecimal.class))
                .isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(jdbc.queryForObject("SELECT receiver_name FROM orders", String.class))
                .isEqualTo("Nguyen Van A");
    }

    @Test
    void changingThePriceAfterOrderingDoesNotChangeTheOrderLine() {
        long variantId = createVariant("100000.00", 5);
        long addressId = prepareCustomerWithCart("buyer@example.com", variantId, 1);
        placeOrder(addressId, null);

        jdbc.update("UPDATE product_variants SET price = 999999 WHERE id = ?", variantId);

        assertThat(jdbc.queryForObject("SELECT unit_price FROM order_items", BigDecimal.class))
                .isEqualByComparingTo(new BigDecimal("100000"));
    }

    // ------------------------------------------------------------------
    // O43: lỗi giữa chừng thì rollback toàn bộ
    // ------------------------------------------------------------------

    @Test
    void stockRunningOutBeforeCheckoutLeavesTheDatabaseUntouched() {
        long variantId = createVariant("100000.00", 5);
        long addressId = prepareCustomerWithCart("buyer@example.com", variantId, 3);

        // Có người khác mua mất hàng trước khi khách này bấm đặt.
        jdbc.update("UPDATE product_variants SET stock = 1 WHERE id = ?", variantId);

        assertThatThrownBy(() -> placeOrder(addressId, null))
                .isInstanceOf(BadRequestException.class);

        assertThat(stockOf(variantId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM orders")).isZero();
        assertThat(jdbc.queryForObject("SELECT quantity FROM cart_items", Integer.class)).isEqualTo(3);
    }

    @Test
    void databaseFailureWhileSavingThePaymentRollsBackOrderItemsAndStock() {
        long variantId = createVariant("100000.00", 5);
        long addressId = prepareCustomerWithCart("buyer@example.com", variantId, 2);

        failOnInsertInto("payments", () ->
                assertThatThrownBy(() -> placeOrder(addressId, null))
                        .isInstanceOf(RuntimeException.class)
                        .isNotInstanceOf(BadRequestException.class));

        assertThat(stockOf(variantId)).isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM orders")).isZero();
        assertThat(count("SELECT COUNT(*) FROM order_items")).isZero();
        assertThat(count("SELECT COUNT(*) FROM payments")).isZero();
        assertThat(jdbc.queryForObject("SELECT quantity FROM cart_items", Integer.class)).isEqualTo(2);
    }

    @Test
    void databaseFailureWhileRecordingTheVoucherRollsBackTheUsageCounterToo() {
        long variantId = createVariant("100000.00", 5);
        createVoucher("SAVE10", "FIXED", "10000");
        long addressId = prepareCustomerWithCart("buyer@example.com", variantId, 2);

        failOnInsertInto("voucher_usages", () ->
                assertThatThrownBy(() -> placeOrder(addressId, "SAVE10"))
                        .isInstanceOf(RuntimeException.class)
                        .isNotInstanceOf(BadRequestException.class));

        assertThat(stockOf(variantId)).isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM orders")).isZero();
        assertThat(count("SELECT used_count FROM vouchers WHERE code = 'SAVE10'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM voucher_usages")).isZero();
        assertThat(count("SELECT COUNT(*) FROM cart_items")).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // O47: nhiều người cùng mua vượt tồn kho
    // ------------------------------------------------------------------

    @Test
    void manyCustomersBuyingTheLastItemsAtTheSameTimeNeverOversell() throws Exception {
        long variantId = createVariant("100000.00", 3);

        List<Callable<OrderResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String email = "buyer" + i + "@example.com";
            long addressId = prepareCustomerWithCart(email, variantId, 1);
            SecurityContextHolder.clearContext();

            tasks.add(() -> {
                authenticateAs(email, "CUSTOMER");
                try {
                    return placeOrder(addressId, null);
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
        }

        List<Object> outcomes = runConcurrently(tasks);

        long successes = outcomes.stream().filter(o -> o instanceof OrderResponse).count();
        List<Object> failures = outcomes.stream().filter(o -> !(o instanceof OrderResponse)).toList();

        assertThat(successes).isEqualTo(3);
        assertThat(failures).hasSize(5)
                .allSatisfy(failure -> assertThat(failure).isInstanceOf(BadRequestException.class));
        assertThat(stockOf(variantId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM orders")).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM order_items")).isEqualTo(3);
    }

    @Test
    void aVoucherWithOneUseLeftCanOnlyBeUsedByOneOfTwoConcurrentOrders() throws Exception {
        long variantId = createVariant("100000.00", 10);
        createVoucher("ONLYONE", "FIXED", "10000", "0", null, 1, -1, 24);

        List<Callable<OrderResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            String email = "buyer" + i + "@example.com";
            long addressId = prepareCustomerWithCart(email, variantId, 1);
            SecurityContextHolder.clearContext();

            tasks.add(() -> {
                authenticateAs(email, "CUSTOMER");
                try {
                    return placeOrder(addressId, "ONLYONE");
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
        }

        List<Object> outcomes = runConcurrently(tasks);

        assertThat(outcomes.stream().filter(o -> o instanceof OrderResponse).count()).isEqualTo(1);
        assertThat(outcomes.stream().filter(o -> o instanceof BadRequestException).count()).isEqualTo(1);

        assertThat(count("SELECT used_count FROM vouchers WHERE code = 'ONLYONE'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM voucher_usages")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM orders")).isEqualTo(1);
        // Đơn thua cuộc đã rollback nên chỉ trừ kho đúng một lần.
        assertThat(stockOf(variantId)).isEqualTo(9);
    }

    @Test
    void doubleClickingPlaceOrderCreatesOnlyOneOrder() throws Exception {
        long variantId = createVariant("100000.00", 10);
        String email = "buyer@example.com";
        long addressId = prepareCustomerWithCart(email, variantId, 2);
        SecurityContextHolder.clearContext();

        List<Callable<OrderResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            tasks.add(() -> {
                authenticateAs(email, "CUSTOMER");
                try {
                    return placeOrder(addressId, null);
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
        }

        List<Object> outcomes = runConcurrently(tasks);

        assertThat(outcomes.stream().filter(o -> o instanceof OrderResponse).count()).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM orders")).isEqualTo(1);
        assertThat(stockOf(variantId)).isEqualTo(8);
    }

    // ------------------------------------------------------------------
    // O44: hủy đơn
    // ------------------------------------------------------------------

    @Test
    void cancellingAnOrderRestoresStockAndGivesTheVoucherBack() {
        long variantId = createVariant("100000.00", 5);
        createVoucher("SAVE10", "FIXED", "10000");
        long addressId = prepareCustomerWithCart("buyer@example.com", variantId, 2);
        OrderResponse order = placeOrder(addressId, "SAVE10");

        assertThat(stockOf(variantId)).isEqualTo(3);
        assertThat(count("SELECT used_count FROM vouchers WHERE code = 'SAVE10'")).isEqualTo(1);

        OrderResponse cancelled = orderService.cancelMine(order.id());

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stockOf(variantId)).isEqualTo(5);
        assertThat(count("SELECT used_count FROM vouchers WHERE code = 'SAVE10'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM voucher_usages")).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT cancelled_at IS NOT NULL FROM orders WHERE id = ?", Boolean.class, order.id()))
                .isTrue();

        // Hủy lần hai bị từ chối và không hoàn kho thêm lần nữa.
        assertThatThrownBy(() -> orderService.cancelMine(order.id()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Order is already cancelled");
        assertThat(stockOf(variantId)).isEqualTo(5);

        // Voucher dùng lại được sau khi đơn cũ bị hủy.
        cartService.addItem(new AddCartItemRequest(variantId, 1));
        OrderResponse again = placeOrder(addressId, "SAVE10");
        assertThat(again.discountAmount()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    void adminCancellingAConfirmedOrderRestoresStockToo() {
        long variantId = createVariant("100000.00", 5);
        long addressId = prepareCustomerWithCart("buyer@example.com", variantId, 4);
        OrderResponse order = placeOrder(addressId, null);
        orderService.updateStatus(order.id(), OrderStatus.CONFIRMED);
        assertThat(stockOf(variantId)).isEqualTo(1);

        orderService.updateStatus(order.id(), OrderStatus.CANCELLED);

        assertThat(orderStatus(order.id())).isEqualTo("CANCELLED");
        assertThat(stockOf(variantId)).isEqualTo(5);
    }

    // ------------------------------------------------------------------
    // O45: chuyển trạng thái
    // ------------------------------------------------------------------

    @Test
    void invalidStatusTransitionsAreRejectedAndTheOrderIsUnchanged() {
        long variantId = createVariant("100000.00", 5);
        long addressId = prepareCustomerWithCart("buyer@example.com", variantId, 1);
        OrderResponse order = placeOrder(addressId, null);

        assertThatThrownBy(() -> orderService.updateStatus(order.id(), OrderStatus.SHIPPING))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> orderService.updateStatus(order.id(), OrderStatus.DELIVERED))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot change order status from PENDING to DELIVERED");
        assertThat(orderStatus(order.id())).isEqualTo("PENDING");

        orderService.updateStatus(order.id(), OrderStatus.CONFIRMED);
        assertThatThrownBy(() -> orderService.updateStatus(order.id(), OrderStatus.PENDING))
                .isInstanceOf(BadRequestException.class);
        assertThat(orderStatus(order.id())).isEqualTo("CONFIRMED");
    }

    @Test
    void adminWalksAnOrderThroughEveryStatusAndCodIsPaidWhenDelivered() {
        long variantId = createVariant("100000.00", 5);
        long addressId = prepareCustomerWithCart("buyer@example.com", variantId, 2);
        OrderResponse order = placeOrder(addressId, null);

        orderService.updateStatus(order.id(), OrderStatus.CONFIRMED);
        orderService.updateStatus(order.id(), OrderStatus.SHIPPING);
        assertThat(jdbc.queryForObject("SELECT status FROM payments", String.class)).isEqualTo("PENDING");

        orderService.updateStatus(order.id(), OrderStatus.DELIVERED);

        assertThat(orderStatus(order.id())).isEqualTo("DELIVERED");
        assertThat(jdbc.queryForObject("SELECT status FROM payments", String.class)).isEqualTo("PAID");
        assertThat(jdbc.queryForObject("SELECT paid_at IS NOT NULL FROM payments", Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT delivered_at IS NOT NULL FROM orders WHERE id = ?", Boolean.class, order.id()))
                .isTrue();

        // Đã giao thì không hủy được nữa, tồn kho không đổi.
        assertThatThrownBy(() -> orderService.cancelMine(order.id()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Order can no longer be cancelled");
        assertThatThrownBy(() -> orderService.updateStatus(order.id(), OrderStatus.CANCELLED))
                .isInstanceOf(BadRequestException.class);
        assertThat(stockOf(variantId)).isEqualTo(3);
    }

    // ------------------------------------------------------------------

    /** Tạm gắn một trigger làm hỏng mọi lệnh INSERT vào bảng, để mô phỏng lỗi database. */
    private void failOnInsertInto(String table, Runnable action) {
        jdbc.execute("""
                CREATE OR REPLACE FUNCTION test_fail_insert() RETURNS trigger AS $$
                BEGIN
                    RAISE EXCEPTION 'simulated database failure';
                END;
                $$ LANGUAGE plpgsql
                """);
        jdbc.execute("CREATE TRIGGER test_fail_insert BEFORE INSERT ON " + table
                + " FOR EACH ROW EXECUTE FUNCTION test_fail_insert()");
        try {
            action.run();
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS test_fail_insert ON " + table);
        }
    }
}
