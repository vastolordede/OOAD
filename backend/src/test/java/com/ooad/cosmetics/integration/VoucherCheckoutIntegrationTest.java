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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** V16: quy tắc nghiệp vụ của Voucher khi thanh toán, trên PostgreSQL thật. */
@Tag("integration")
class VoucherCheckoutIntegrationTest extends IntegrationTestBase {

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private OrderService orderService;

    private OrderResponse placeOrder(long addressId, String voucherCode) {
        return checkoutService.createOrder(
                new CreateOrderRequest(addressId, voucherCode, PaymentMethod.COD, null)
        );
    }

    @Test
    void percentageVoucherIsCappedByMaxDiscountAndUsageIsRecorded() {
        long variantId = createVariant("500000.00", 5);
        createVoucher("BIG20", "PERCENTAGE", "20", "0", "50000", null, -1, 24);
        long addressId = prepareCustomerWithCart("buyer@example.com", variantId, 2);

        OrderResponse order = placeOrder(addressId, "BIG20");

        // 20% của 1.000.000 là 200.000 nhưng bị chặn ở 50.000; đơn từ 500.000 được miễn phí ship.
        assertThat(order.subtotal()).isEqualByComparingTo(new BigDecimal("1000000"));
        assertThat(order.discountAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(order.shippingFee()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.totalAmount()).isEqualByComparingTo(new BigDecimal("950000"));
        assertThat(order.voucherCode()).isEqualTo("BIG20");

        assertThat(count("SELECT used_count FROM vouchers WHERE code = 'BIG20'")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT discount_amount FROM voucher_usages", BigDecimal.class))
                .isEqualByComparingTo(new BigDecimal("50000"));
    }

    @Test
    void voucherCodeIsCaseInsensitiveAndFixedDiscountNeverExceedsTheSubtotal() {
        long variantId = createVariant("20000.00", 5);
        createVoucher("HUGE", "FIXED", "100000");
        long addressId = prepareCustomerWithCart("buyer@example.com", variantId, 1);

        OrderResponse order = placeOrder(addressId, "  huge ");

        assertThat(order.discountAmount()).isEqualByComparingTo(new BigDecimal("20000"));
        // Tiền hàng được giảm hết, khách chỉ còn trả phí ship.
        assertThat(order.totalAmount()).isEqualByComparingTo(new BigDecimal("30000"));
    }

    @Test
    void orderBelowTheMinimumValueIsRejectedWithoutSideEffects() {
        long variantId = createVariant("100000.00", 5);
        createVoucher("MIN300", "FIXED", "10000", "300000", null, null, -1, 24);
        long addressId = prepareCustomerWithCart("buyer@example.com", variantId, 2);

        assertThatThrownBy(() -> placeOrder(addressId, "MIN300"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("below the voucher minimum");

        assertNothingWasOrdered(variantId, 5);
    }

    @Test
    void expiredVoucherIsRejected() {
        long variantId = createVariant("100000.00", 5);
        createVoucher("OLD", "FIXED", "10000", "0", null, null, -48, -24);
        long addressId = prepareCustomerWithCart("buyer@example.com", variantId, 1);

        assertThatThrownBy(() -> placeOrder(addressId, "OLD"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Voucher has expired");

        assertNothingWasOrdered(variantId, 5);
    }

    @Test
    void voucherThatHasNotStartedYetIsRejected() {
        long variantId = createVariant("100000.00", 5);
        createVoucher("SOON", "FIXED", "10000", "0", null, null, 24, 48);
        long addressId = prepareCustomerWithCart("buyer@example.com", variantId, 1);

        assertThatThrownBy(() -> placeOrder(addressId, "SOON"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Voucher is not yet valid");

        assertNothingWasOrdered(variantId, 5);
    }

    @Test
    void inactiveVoucherIsRejected() {
        long variantId = createVariant("100000.00", 5);
        createVoucher("OFF", "FIXED", "10000");
        jdbc.update("UPDATE vouchers SET status = 'INACTIVE' WHERE code = 'OFF'");
        long addressId = prepareCustomerWithCart("buyer@example.com", variantId, 1);

        assertThatThrownBy(() -> placeOrder(addressId, "OFF"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Voucher is not active");

        assertNothingWasOrdered(variantId, 5);
    }

    @Test
    void unknownVoucherCodeIsRejected() {
        long variantId = createVariant("100000.00", 5);
        long addressId = prepareCustomerWithCart("buyer@example.com", variantId, 1);

        assertThatThrownBy(() -> placeOrder(addressId, "DOESNOTEXIST"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Voucher code is invalid");

        assertNothingWasOrdered(variantId, 5);
    }

    @Test
    void aCustomerCannotUseTheSameVoucherTwiceButAnotherCustomerCan() {
        long variantId = createVariant("100000.00", 10);
        createVoucher("SAVE10", "FIXED", "10000");

        long addressId = prepareCustomerWithCart("buyer@example.com", variantId, 1);
        placeOrder(addressId, "SAVE10");
        assertThat(stockOf(variantId)).isEqualTo(9);

        cartService.addItem(new AddCartItemRequest(variantId, 1));
        assertThatThrownBy(() -> placeOrder(addressId, "SAVE10"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("You have already used this voucher");
        // Đơn bị từ chối: kho không bị trừ thêm, hàng vẫn còn trong giỏ.
        assertThat(stockOf(variantId)).isEqualTo(9);
        assertThat(count("SELECT COUNT(*) FROM cart_items")).isEqualTo(1);

        long otherAddressId = prepareCustomerWithCart("other@example.com", variantId, 1);
        OrderResponse order = placeOrder(otherAddressId, "SAVE10");
        assertThat(order.discountAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(count("SELECT used_count FROM vouchers WHERE code = 'SAVE10'")).isEqualTo(2);
    }

    @Test
    void voucherWithUsageLimitIsRejectedOnceTheLimitIsReached() {
        long variantId = createVariant("100000.00", 10);
        createVoucher("TWICE", "FIXED", "10000", "0", null, 2, -1, 24);

        for (int i = 0; i < 2; i++) {
            long addressId = prepareCustomerWithCart("buyer" + i + "@example.com", variantId, 1);
            placeOrder(addressId, "TWICE");
        }

        long thirdAddressId = prepareCustomerWithCart("buyer2@example.com", variantId, 1);
        assertThatThrownBy(() -> placeOrder(thirdAddressId, "TWICE"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Voucher usage limit reached");

        assertThat(count("SELECT used_count FROM vouchers WHERE code = 'TWICE'")).isEqualTo(2);
        assertThat(stockOf(variantId)).isEqualTo(8);
    }

    @Test
    void cancellingAnOrderReleasesVoucherUsageSoTheCustomerCanUseItAgain() {
        long variantId = createVariant("100000.00", 5);
        createVoucher("REUSE", "FIXED", "10000");
        long addressId = prepareCustomerWithCart("buyer@example.com", variantId, 1);

        OrderResponse firstOrder = placeOrder(addressId, "REUSE");
        assertThat(count("SELECT used_count FROM vouchers WHERE code = 'REUSE'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM voucher_usages")).isEqualTo(1);

        orderService.updateStatus(firstOrder.id(), OrderStatus.CANCELLED);

        assertThat(count("SELECT used_count FROM vouchers WHERE code = 'REUSE'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM voucher_usages")).isZero();

        cartService.addItem(new AddCartItemRequest(variantId, 1));
        OrderResponse secondOrder = placeOrder(addressId, "REUSE");

        assertThat(secondOrder.voucherCode()).isEqualTo("REUSE");
        assertThat(count("SELECT used_count FROM vouchers WHERE code = 'REUSE'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM voucher_usages")).isEqualTo(1);
    }

    // ------------------------------------------------------------------

    private void assertNothingWasOrdered(long variantId, int expectedStock) {
        assertThat(stockOf(variantId)).isEqualTo(expectedStock);
        assertThat(count("SELECT COUNT(*) FROM orders")).isZero();
        assertThat(count("SELECT COUNT(*) FROM voucher_usages")).isZero();
        assertThat(count("SELECT COUNT(*) FROM cart_items")).isEqualTo(1);
    }
}
