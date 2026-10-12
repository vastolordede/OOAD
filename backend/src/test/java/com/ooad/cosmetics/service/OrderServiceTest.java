package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.order.OrderResponse;
import com.ooad.cosmetics.dto.order.OrderSummaryResponse;
import com.ooad.cosmetics.entity.Order;
import com.ooad.cosmetics.entity.OrderItem;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.entity.Payment;
import com.ooad.cosmetics.entity.PaymentMethod;
import com.ooad.cosmetics.entity.PaymentStatus;
import com.ooad.cosmetics.entity.ProductVariant;
import com.ooad.cosmetics.entity.User;
import com.ooad.cosmetics.repository.OrderItemRepository;
import com.ooad.cosmetics.repository.OrderRepository;
import com.ooad.cosmetics.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private PaymentRepository paymentRepository;
    private StockService stockService;
    private VoucherService voucherService;
    private OrderService service;
    private User user;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        stockService = mock(StockService.class);
        voucherService = mock(VoucherService.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);

        service = new OrderService(
                orderRepository,
                orderItemRepository,
                paymentRepository,
                stockService,
                voucherService,
                currentUserService
        );

        user = new User("customer@example.com", "hash", "Customer", null);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
    }

    // ---------------- customer: history / detail ----------------

    @Test
    void listMineBuildsSummariesWithBatchQueries() {
        Order order = order(10L, OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("230000.00"));
        OrderItem first = orderItem(order, 10L, 2, "Serum");
        OrderItem second = orderItem(order, 11L, 1, "Cream");
        Payment payment = payment(order, PaymentMethod.COD, PaymentStatus.PENDING);

        when(orderRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1));
        when(orderItemRepository.findByOrderIdInOrderByIdAsc(List.of(10L)))
                .thenReturn(List.of(first, second));
        when(paymentRepository.findByOrderIdIn(List.of(10L)))
                .thenReturn(List.of(payment));

        PageResponse<OrderSummaryResponse> page = service.listMine(null, 0, 10);

        assertThat(page.totalElements()).isEqualTo(1);
        OrderSummaryResponse summary = page.content().get(0);
        assertThat(summary.id()).isEqualTo(10L);
        assertThat(summary.itemCount()).isEqualTo(3);
        assertThat(summary.firstItemName()).isEqualTo("Serum");
        assertThat(summary.customerEmail()).isEqualTo("customer@example.com");
        assertThat(summary.paymentMethod()).isEqualTo(PaymentMethod.COD);
        assertThat(summary.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(summary.totalAmount()).isEqualByComparingTo(new BigDecimal("230000"));
    }

    @Test
    void detailMineRejectsOrderOfAnotherCustomer() {
        when(orderRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detailMine(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------- customer: cancel ----------------

    @Test
    void cancelMineRestoresStockInVariantOrderReleasesVoucherAndMarksCancelled() {
        Order order = order(10L, OrderStatus.PENDING);
        when(orderRepository.lockByIdAndUserId(10L, 1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(10L)).thenReturn(List.of(
                orderItem(order, 20L, 2, "B"),
                orderItem(order, 10L, 1, "A")
        ));
        Payment payment = payment(order, PaymentMethod.COD, PaymentStatus.PENDING);
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(payment));

        OrderResponse response = service.cancelMine(10L);

        InOrder inOrder = inOrder(stockService);
        inOrder.verify(stockService).restore(10L, 1);
        inOrder.verify(stockService).restore(20L, 2);
        verify(voucherService).releaseUsage(order);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isNotNull();
        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void cancelMineIsAllowedWhenOrderIsConfirmed() {
        Order order = order(10L, OrderStatus.CONFIRMED);
        when(orderRepository.lockByIdAndUserId(10L, 1L)).thenReturn(Optional.of(order));

        OrderResponse response = service.cancelMine(10L);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelMineRefundsAPaidPayment() {
        Order order = order(10L, OrderStatus.CONFIRMED);
        when(orderRepository.lockByIdAndUserId(10L, 1L)).thenReturn(Optional.of(order));
        Payment payment = payment(order, PaymentMethod.ONLINE, PaymentStatus.PAID);
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(payment));

        service.cancelMine(10L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"SHIPPING", "DELIVERED"})
    void cancelMineRejectsOrdersThatAreAlreadyShippedOrDelivered(OrderStatus status) {
        Order order = order(10L, status);
        when(orderRepository.lockByIdAndUserId(10L, 1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelMine(10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Order can no longer be cancelled");

        assertThat(order.getStatus()).isEqualTo(status);
        verifyNoInteractions(stockService);
        verifyNoInteractions(voucherService);
    }

    @Test
    void cancelMineRejectsAnOrderThatIsAlreadyCancelled() {
        Order order = order(10L, OrderStatus.CANCELLED);
        when(orderRepository.lockByIdAndUserId(10L, 1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelMine(10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Order is already cancelled");

        verifyNoInteractions(stockService);
    }

    @Test
    void cancelMineRejectsOrderOfAnotherCustomer() {
        when(orderRepository.lockByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelMine(10L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(stockService);
    }

    // ---------------- admin: status transitions ----------------

    @Test
    void updateStatusConfirmsAPendingOrder() {
        Order order = order(10L, OrderStatus.PENDING);
        when(orderRepository.lockById(10L)).thenReturn(Optional.of(order));

        OrderResponse response = service.updateStatus(10L, OrderStatus.CONFIRMED);

        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
        verifyNoInteractions(stockService);
    }

    @Test
    void updateStatusMovesConfirmedOrderToShipping() {
        Order order = order(10L, OrderStatus.CONFIRMED);
        when(orderRepository.lockById(10L)).thenReturn(Optional.of(order));

        OrderResponse response = service.updateStatus(10L, OrderStatus.SHIPPING);

        assertThat(response.status()).isEqualTo(OrderStatus.SHIPPING);
    }

    @Test
    void updateStatusRejectsSkippingAStep() {
        Order order = order(10L, OrderStatus.PENDING);
        when(orderRepository.lockById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.updateStatus(10L, OrderStatus.DELIVERED))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot change order status from PENDING to DELIVERED");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void updateStatusRejectsChangingAFinalStatus() {
        Order order = order(10L, OrderStatus.DELIVERED);
        when(orderRepository.lockById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.updateStatus(10L, OrderStatus.CANCELLED))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot change order status from DELIVERED to CANCELLED");

        verifyNoInteractions(stockService);
        verifyNoInteractions(voucherService);
    }

    @Test
    void updateStatusDeliveredMarksCodPaymentAsPaid() {
        Order order = order(10L, OrderStatus.SHIPPING);
        when(orderRepository.lockById(10L)).thenReturn(Optional.of(order));
        Payment payment = payment(order, PaymentMethod.COD, PaymentStatus.PENDING);
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(payment));

        OrderResponse response = service.updateStatus(10L, OrderStatus.DELIVERED);

        assertThat(response.status()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getDeliveredAt()).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isNotNull();
    }

    @Test
    void updateStatusDeliveredDoesNotTouchAPendingOnlinePayment() {
        Order order = order(10L, OrderStatus.SHIPPING);
        when(orderRepository.lockById(10L)).thenReturn(Optional.of(order));
        Payment payment = payment(order, PaymentMethod.ONLINE, PaymentStatus.PENDING);
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(payment));

        service.updateStatus(10L, OrderStatus.DELIVERED);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPaidAt()).isNull();
    }

    @Test
    void updateStatusCancelledByAdminRestoresStock() {
        Order order = order(10L, OrderStatus.PENDING);
        when(orderRepository.lockById(10L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(10L))
                .thenReturn(List.of(orderItem(order, 10L, 3, "A")));

        OrderResponse response = service.updateStatus(10L, OrderStatus.CANCELLED);

        verify(stockService).restore(10L, 3);
        verify(voucherService).releaseUsage(order);
        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isNotNull();
    }

    @Test
    void updateStatusRejectsUnknownOrder() {
        when(orderRepository.lockById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(99L, OrderStatus.CONFIRMED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void adminListRejectsFromDateAfterToDate() {
        assertThatThrownBy(() -> service.adminList(
                null, null, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1), 0, 20))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("from must not be after to");
    }

    // ---------------- online payment must be completed before confirming ----------------

    @Test
    void updateStatusRefusesToConfirmAnOnlineOrderThatIsNotPaidYet() {
        Order order = order(10L, OrderStatus.PENDING);
        when(orderRepository.lockById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(10L))
                .thenReturn(Optional.of(payment(order, PaymentMethod.ONLINE, PaymentStatus.PENDING)));

        assertThatThrownBy(() -> service.updateStatus(10L, OrderStatus.CONFIRMED))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Online payment has not been completed");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void updateStatusRefusesToConfirmAnOnlineOrderWhosePaymentFailed() {
        Order order = order(10L, OrderStatus.PENDING);
        when(orderRepository.lockById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(10L))
                .thenReturn(Optional.of(payment(order, PaymentMethod.ONLINE, PaymentStatus.FAILED)));

        assertThatThrownBy(() -> service.updateStatus(10L, OrderStatus.CONFIRMED))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Online payment has not been completed");
    }

    @Test
    void updateStatusConfirmsAnOnlineOrderOnceItIsPaid() {
        Order order = order(10L, OrderStatus.PENDING);
        when(orderRepository.lockById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(10L))
                .thenReturn(Optional.of(payment(order, PaymentMethod.ONLINE, PaymentStatus.PAID)));

        OrderResponse response = service.updateStatus(10L, OrderStatus.CONFIRMED);

        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void updateStatusConfirmsACodOrderWithoutWaitingForPayment() {
        Order order = order(10L, OrderStatus.PENDING);
        when(orderRepository.lockById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(10L))
                .thenReturn(Optional.of(payment(order, PaymentMethod.COD, PaymentStatus.PENDING)));

        OrderResponse response = service.updateStatus(10L, OrderStatus.CONFIRMED);

        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void updateStatusStillAllowsCancellingAnUnpaidOnlineOrder() {
        Order order = order(10L, OrderStatus.PENDING);
        when(orderRepository.lockById(10L)).thenReturn(Optional.of(order));
        Payment payment = payment(order, PaymentMethod.ONLINE, PaymentStatus.PENDING);
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(payment));

        OrderResponse response = service.updateStatus(10L, OrderStatus.CANCELLED);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    // ---------------- helpers ----------------

    private Order order(Long id, OrderStatus status) {
        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", id);
        order.setUser(user);
        order.setStatus(status);
        order.setReceiverName("Nguyen Van A");
        return order;
    }

    private OrderItem orderItem(Order order, Long variantId, int quantity, String name) {
        ProductVariant variant = new ProductVariant();
        ReflectionTestUtils.setField(variant, "id", variantId);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setVariant(variant);
        item.setQuantity(quantity);
        item.setProductName(name);
        return item;
    }

    private Payment payment(Order order, PaymentMethod method, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(method);
        payment.setStatus(status);
        return payment;
    }
}
