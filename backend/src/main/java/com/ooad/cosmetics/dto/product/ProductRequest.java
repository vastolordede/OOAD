package com.ooad.cosmetics.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 10000)
        String description,

        @NotNull
        Long categoryId,

        @NotNull
        Long brandId
) {
}
