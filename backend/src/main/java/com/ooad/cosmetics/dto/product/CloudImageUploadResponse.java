package com.ooad.cosmetics.dto.product;

public record CloudImageUploadResponse(
        String imageUrl,
        String publicId,
        ProductImageResponse productImage
) {
}
