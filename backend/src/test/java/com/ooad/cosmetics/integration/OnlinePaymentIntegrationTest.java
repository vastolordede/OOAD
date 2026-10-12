package com.ooad.cosmetics.integration;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.dto.order.CreateOrderRequest;
import com.ooad.cosmetics.dto.order.OrderResponse;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.entity.PaymentMethod;
import com.ooad.cosmetics.service.CheckoutService;
import com.ooad.cosmetics.service.OrderService;
import com.ooad.cosmetics.service.PaymentService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O48: thanh toán online mô phỏng, chạy trên PostgreSQL thật. */
@Tag("integration")
class OnlinePaymentIntegrationTest extends IntegrationTestBase {

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    private OrderResponse placeOrder(String email, long variantId, int quantity, PaymentMethod method) {
        long addressId = prepareCustomerWithCart(email, variantId, quantity);
        return checkoutService.createOrder(new CreateOrderRequest(addressId, null, method, null));
    }

    private String paymentStatus() {
        return jdbc.queryForObject("SELECT status FROM payments", String.class);
    }

    @Test
    void anOnlineOrderStartsWithAPendingPaymentAndAMockReference() {
        long variantId = createVariant("100000.00", 5);

        OrderResponse order = placeOrder("buyer@example.com", variantId, 2, PaymentMethod.ONLINE);

        assertThat(order.payment().method()).isEqualTo(PaymentMethod.ONLINE);
        assertThat(order.payment().transactionRef()).startsWith("MOCK-").hasSize(25);
        assertThat(jdbc.queryForObject("SELECT method FROM payments", String.class)).isEqualTo("ONLINE");
        assertThat(paymentStatus()).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT paid_at IS NULL FROM payments", Boolean.class)).isTrue();
        assertThat(stockOf(variantId)).isEqualTo(3);
    }

    @Test
    void aCodOrderHasNoTransactionReference() {
        long variantId = createVariant("100000.00", 5);

        placeOrder("buyer@example.com", variantId, 1, PaymentMethod.COD);

        assertThat(jdbc.queryForObject("SELECT transaction_ref IS NULL FROM payments", Boolean.class)).isTrue();
    }

    @Test
    void anUnpaidOnlineOrderCannotBeConfirmedUntilThePaymentSucceeds() {
        long variantId = createVariant("100000.00", 5);
        OrderResponse order = placeOrder("buyer@example.com", variantId, 1, PaymentMethod.ONLINE);

        assertThatThrownBy(() -> orderService.updateStatus(order.id(), OrderStatus.CONFIRMED))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Online payment has not been completed");
        assertThat(orderStatus(order.id())).isEqualTo("PENDING");

        paymentService.confirm(order.id());
        assertThat(paymentStatus()).isEqualTo("PAID");
        assertThat(jdbc.queryForObject("SELECT paid_at IS NOT NULL FROM payments", Boolean.class)).isTrue();

        orderService.updateStatus(order.id(), OrderStatus.CONFIRMED);
        orderService.updateStatus(order.id(), OrderStatus.SHIPPING);
        orderService.updateStatus(order.id(), OrderStatus.DELIVERED);
        assertThat(orderStatus(order.id())).isEqualTo("DELIVERED");
        assertThat(paymentStatus()).isEqualTo("PAID");
    }

    @Test
    void aFailedPaymentCanBeRetriedAndTheOrderStaysPendingMeanwhile() {
        long variantId = createVariant("100000.00", 5);
        OrderResponse order = placeOrder("buyer@example.com", variantId, 1, PaymentMethod.ONLINE);

        paymentService.fail(order.id());
        assertThat(paymentStatus()).isEqualTo("FAILED");
        assertThat(orderStatus(order.id())).isEqualTo("PENDING");

        assertThatThrownBy(() -> orderService.updateStatus(order.id(), OrderStatus.CONFIRMED))
                .isInstanceOf(BadRequestException.class);

        paymentService.confirm(order.id());
        assertThat(paymentStatus()).isEqualTo("PAID");
    }

    @Test
    void aPaidOrderCannotBePaidAgainOrMarkedAsFailed() {
        long variantId = createVariant("100000.00", 5);
        OrderResponse order = placeOrder("buyer@example.com", variantId, 1, PaymentMethod.ONLINE);
        paymentService.confirm(order.id());

        assertThatThrownBy(() -> paymentService.confirm(order.id()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Order is already paid");
        assertThatThrownBy(() -> paymentService.fail(order.id()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Order is already paid");
        assertThat(paymentStatus()).isEqualTo("PAID");
    }

    @Test
    void cancellingAPaidOnlineOrderRefundsThePaymentAndRestoresStock() {
        long variantId = createVariant("100000.00", 5);
        OrderResponse order = placeOrder("buyer@example.com", variantId, 2, PaymentMethod.ONLINE);
        paymentService.confirm(order.id());
        assertThat(stockOf(variantId)).isEqualTo(3);

        orderService.cancelMine(order.id());

        assertThat(orderStatus(order.id())).isEqualTo("CANCELLED");
        assertThat(paymentStatus()).isEqualTo("REFUNDED");
        assertThat(stockOf(variantId)).isEqualTo(5);
    }

    @Test
    void cancellingAnUnpaidOnlineOrderStillWorksAndTheCancelledOrderCannotBePaid() {
        long variantId = createVariant("100000.00", 5);
        OrderResponse order = placeOrder("buyer@example.com", variantId, 2, PaymentMethod.ONLINE);

        orderService.cancelMine(order.id());

        assertThat(stockOf(variantId)).isEqualTo(5);
        assertThat(paymentStatus()).isEqualTo("PENDING");
        assertThatThrownBy(() -> paymentService.confirm(order.id()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Order is not awaiting payment");
        assertThat(paymentStatus()).isEqualTo("PENDING");
    }

    @Test
    void anotherCustomerCannotPayOrReadMyPayment() {
        long variantId = createVariant("100000.00", 5);
        OrderResponse order = placeOrder("owner@example.com", variantId, 1, PaymentMethod.ONLINE);

        createCustomer("stranger@example.com");
        authenticateAs("stranger@example.com", "CUSTOMER");

        assertThatThrownBy(() -> paymentService.confirm(order.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> paymentService.get(order.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(paymentStatus()).isEqualTo("PENDING");
    }

    @Test
    void aCodOrderCannotBePaidOnline() {
        long variantId = createVariant("100000.00", 5);
        OrderResponse order = placeOrder("buyer@example.com", variantId, 1, PaymentMethod.COD);

        assertThatThrownBy(() -> paymentService.confirm(order.id()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This order is not paid online");
        assertThat(paymentStatus()).isEqualTo("PENDING");
    }
}
