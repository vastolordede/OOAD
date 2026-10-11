package com.ooad.cosmetics.dto.order;

import com.ooad.cosmetics.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull
        OrderStatus status
) {
}
