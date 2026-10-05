package com.ooad.cosmetics.dto.catalog;

import com.ooad.cosmetics.entity.Brand;
import com.ooad.cosmetics.entity.CatalogStatus;

public record BrandResponse(
        Long id,
        String name,
        CatalogStatus status
) {
    public static BrandResponse from(Brand brand) {
        return new BrandResponse(
                brand.getId(),
                brand.getName(),
                brand.getStatus()
        );
    }
}
