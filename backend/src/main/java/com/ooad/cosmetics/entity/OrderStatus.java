package com.ooad.cosmetics.entity;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPING,
    DELIVERED,
    CANCELLED;

    /**
     * Các chuyển trạng thái hợp lệ (O26 - O30):
     * PENDING -> CONFIRMED | CANCELLED
     * CONFIRMED -> SHIPPING | CANCELLED
     * SHIPPING -> DELIVERED
     * DELIVERED, CANCELLED: trạng thái cuối, không đổi nữa.
     */
    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == SHIPPING || next == CANCELLED;
            case SHIPPING -> next == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
