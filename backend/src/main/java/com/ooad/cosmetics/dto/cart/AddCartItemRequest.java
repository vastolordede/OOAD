package com.ooad.cosmetics.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(
        @NotNull
        Long variantId,

        @Min(1)
        @Max(1000)
        int quantity
) {
}
