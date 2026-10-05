package com.ooad.cosmetics.dto.product;

import com.ooad.cosmetics.entity.CatalogStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductDetailResponse(
        Long id,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        Long brandId,
        String brandName,
        CatalogStatus status,
        BigDecimal minPrice,
        List<ProductVariantResponse> variants,
        List<ProductImageResponse> images,
        Instant createdAt,
        Instant updatedAt
) {
}
