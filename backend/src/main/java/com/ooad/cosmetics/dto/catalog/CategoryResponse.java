package com.ooad.cosmetics.dto.catalog;

import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.entity.Category;

public record CategoryResponse(
        Long id,
        String name,
        CatalogStatus status
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getStatus()
        );
    }
}
