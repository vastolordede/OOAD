package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.dto.order.PaymentResponse;
import com.ooad.cosmetics.entity.Order;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.entity.Payment;
import com.ooad.cosmetics.entity.PaymentMethod;
import com.ooad.cosmetics.entity.PaymentStatus;
import com.ooad.cosmetics.entity.User;
import com.ooad.cosmetics.repository.OrderRepository;
import com.ooad.cosmetics.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private PaymentService service;
    private User user;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        service = new PaymentService(orderRepository, paymentRepository, currentUserService);

        user = new User("customer@example.com", "hash", "Customer", null);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
    }

    // ---------------- confirm ----------------

    @Test
    void confirmMarksAnOnlinePaymentAsPaid() {
        Payment payment = stub(OrderStatus.PENDING, PaymentMethod.ONLINE, PaymentStatus.PENDING);

        PaymentResponse response = service.confirm(10L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.transactionRef()).isEqualTo("MOCK-REF");
    }

    @Test
    void confirmCanRetryAfterAFailedAttempt() {
        Payment payment = stub(OrderStatus.PENDING, PaymentMethod.ONLINE, PaymentStatus.FAILED);

        service.confirm(10L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void confirmRejectsAnOrderThatIsAlreadyPaid() {
        stub(OrderStatus.PENDING, PaymentMethod.ONLINE, PaymentStatus.PAID);

        assertThatThrownBy(() -> service.confirm(10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Order is already paid");
    }

    @Test
    void confirmRejectsAnOrderThatIsPaidOnDelivery() {
        Payment payment = stub(OrderStatus.PENDING, PaymentMethod.COD, PaymentStatus.PENDING);

        assertThatThrownBy(() -> service.confirm(10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This order is not paid online");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @ParameterizedTest
    @EnumSource(
            value = OrderStatus.class,
            names = {"CONFIRMED", "SHIPPING", "DELIVERED", "CANCELLED"}
    )
    void confirmRejectsOrdersThatAreNoLongerAwaitingPayment(OrderStatus status) {
        Payment payment = stub(status, PaymentMethod.ONLINE, PaymentStatus.PENDING);

        assertThatThrownBy(() -> service.confirm(10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Order is not awaiting payment");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void confirmRejectsTheOrderOfAnotherCustomer() {
        when(orderRepository.lockByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------- fail ----------------

    @Test
    void failMarksThePaymentAsFailed() {
        Payment payment = stub(OrderStatus.PENDING, PaymentMethod.ONLINE, PaymentStatus.PENDING);

        PaymentResponse response = service.fail(10L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getPaidAt()).isNull();
        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void failRejectsAnOrderThatIsAlreadyPaid() {
        Payment payment = stub(OrderStatus.PENDING, PaymentMethod.ONLINE, PaymentStatus.PAID);

        assertThatThrownBy(() -> service.fail(10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Order is already paid");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    // ---------------- get ----------------

    @Test
    void getReturnsThePaymentOfMyOrder() {
        Order order = order(OrderStatus.PENDING);
        Payment payment = payment(order, PaymentMethod.ONLINE, PaymentStatus.PENDING);
        when(orderRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(payment));

        PaymentResponse response = service.get(10L);

        assertThat(response.method()).isEqualTo(PaymentMethod.ONLINE);
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("230000"));
    }

    @Test
    void getRejectsTheOrderOfAnotherCustomer() {
        when(orderRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------- helpers ----------------

    private Payment stub(OrderStatus orderStatus, PaymentMethod method, PaymentStatus paymentStatus) {
        Order order = order(orderStatus);
        Payment payment = payment(order, method, paymentStatus);
        when(orderRepository.lockByIdAndUserId(10L, 1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(payment));
        return payment;
    }

    private Order order(OrderStatus status) {
        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", 10L);
        order.setUser(user);
        order.setStatus(status);
        return order;
    }

    private Payment payment(Order order, PaymentMethod method, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(method);
        payment.setStatus(status);
        payment.setAmount(new BigDecimal("230000.00"));
        payment.setTransactionRef("MOCK-REF");
        return payment;
    }
}
