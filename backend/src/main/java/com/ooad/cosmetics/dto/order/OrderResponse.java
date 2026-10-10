package com.ooad.cosmetics.dto.order;

import com.ooad.cosmetics.entity.Order;
import com.ooad.cosmetics.entity.OrderItem;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.entity.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,
        String receiverName,
        String receiverPhone,
        String shippingAddressLine,
        String shippingWard,
        String shippingDistrict,
        String shippingCity,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String voucherCode,
        String note,
        PaymentResponse payment,
        Instant createdAt,
        Instant deliveredAt,
        Instant cancelledAt
) {
    public static OrderResponse from(Order order, List<OrderItem> items, Payment payment) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getShippingAddressLine(),
                order.getShippingWard(),
                order.getShippingDistrict(),
                order.getShippingCity(),
                items.stream().map(OrderItemResponse::from).toList(),
                order.getSubtotal(),
                order.getShippingFee(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getVoucherCode(),
                order.getNote(),
                payment == null ? null : PaymentResponse.from(payment),
                order.getCreatedAt(),
                order.getDeliveredAt(),
                order.getCancelledAt()
        );
    }
}
