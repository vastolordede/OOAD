package com.ooad.cosmetics.dto.order;

public record AdminOrderResponse(
        Long customerId,
        String customerEmail,
        String customerName,
        OrderResponse order
) {
}
