package com.ooad.cosmetics.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductVariantRequest(
        @NotBlank
        @Size(max = 100)
        String sku,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal price,

        @Min(0)
        int stock
) {
}
