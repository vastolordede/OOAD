package com.ooad.cosmetics.dto.product;

import com.ooad.cosmetics.entity.ProductImage;

public record ProductImageResponse(
        Long id,
        String imageUrl,
        boolean primaryImage,
        int sortOrder
) {
    public static ProductImageResponse from(ProductImage image) {
        return new ProductImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.isPrimaryImage(),
                image.getSortOrder()
        );
    }
}
