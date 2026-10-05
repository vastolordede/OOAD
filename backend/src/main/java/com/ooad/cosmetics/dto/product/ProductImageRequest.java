package com.ooad.cosmetics.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductImageRequest(
        @NotBlank
        @Size(max = 1000)
        String imageUrl,

        @Min(0)
        int sortOrder
) {
}
