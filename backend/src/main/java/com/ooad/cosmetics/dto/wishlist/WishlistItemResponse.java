package com.ooad.cosmetics.dto.wishlist;

import java.math.BigDecimal;
import java.time.Instant;

public record WishlistItemResponse(
        Long productId,
        String productName,
        String brandName,
        BigDecimal minPrice,
        String imageUrl,
        boolean available,
        Instant addedAt
) {
}
