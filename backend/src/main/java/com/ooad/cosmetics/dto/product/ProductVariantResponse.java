package com.ooad.cosmetics.dto.product;

import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.entity.ProductVariant;

import java.math.BigDecimal;

public record ProductVariantResponse(
        Long id,
        Long productId,
        String productName,
        String sku,
        BigDecimal price,
        int stock,
        CatalogStatus status
) {
    public static ProductVariantResponse from(ProductVariant variant) {
        return new ProductVariantResponse(
                variant.getId(),
                variant.getProduct().getId(),
                variant.getProduct().getName(),
                variant.getSku(),
                variant.getPrice(),
                variant.getStock(),
                variant.getStatus()
        );
    }
}
