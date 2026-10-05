package com.ooad.cosmetics.dto.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CatalogNameRequest(
        @NotBlank
        @Size(max = 120)
        String name
) {
}
