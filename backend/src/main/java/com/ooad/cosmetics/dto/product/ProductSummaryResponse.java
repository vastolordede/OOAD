package com.ooad.cosmetics.dto.product;

import com.ooad.cosmetics.entity.CatalogStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductSummaryResponse(
        Long id,
        String name,
        Long categoryId,
        String categoryName,
        Long brandId,
        String brandName,
        CatalogStatus status,
        BigDecimal minPrice,
        String primaryImageUrl,
        Instant createdAt
) {
}
