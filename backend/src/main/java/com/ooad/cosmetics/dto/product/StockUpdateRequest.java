package com.ooad.cosmetics.dto.product;

import jakarta.validation.constraints.Min;

public record StockUpdateRequest(
        @Min(0)
        int stock
) {
}
