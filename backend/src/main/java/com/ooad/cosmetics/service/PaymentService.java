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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Thanh toán online MÔ PHỎNG (O48): không kết nối cổng thanh toán thật.
 * Khi đặt đơn ONLINE, hệ thống tạo một mã giao dịch giả. Màn hình "thanh toán" của giao diện
 * gọi confirm (thanh toán thành công) hoặc fail (thất bại) để thay cho cổng thanh toán.
 * Khi nối cổng thật (VNPay, MoMo...), chỉ cần thay hai hàm này bằng xử lý callback của cổng.
 */
@Service
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentUserService currentUserService;

    public PaymentService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            CurrentUserService currentUserService
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(Long orderId) {
        User user = currentUserService.requireCurrentUser();
        orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        return PaymentResponse.from(
                paymentRepository.findByOrderId(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Payment", orderId))
        );
    }

    /** Thanh toán thành công. Thanh toán đã thất bại trước đó vẫn được thử lại. */
    @Transactional
    public PaymentResponse confirm(Long orderId) {
        Payment payment = requireOnlinePaymentAwaitingCustomer(orderId);

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(Instant.now());
        return PaymentResponse.from(payment);
    }

    /** Thanh toán thất bại. Đơn vẫn còn, khách có thể thử lại hoặc hủy đơn. */
    @Transactional
    public PaymentResponse fail(Long orderId) {
        Payment payment = requireOnlinePaymentAwaitingCustomer(orderId);

        payment.setStatus(PaymentStatus.FAILED);
        return PaymentResponse.from(payment);
    }

    private Payment requireOnlinePaymentAwaitingCustomer(Long orderId) {
        User user = currentUserService.requireCurrentUser();

        // Khóa dòng đơn để hai lần bấm "thanh toán" không chạy chồng lên nhau.
        Order order = orderRepository.lockByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", orderId));

        if (payment.getMethod() != PaymentMethod.ONLINE) {
            throw new BadRequestException("This order is not paid online");
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Order is already paid");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Order is not awaiting payment");
        }
        return payment;
    }
}
