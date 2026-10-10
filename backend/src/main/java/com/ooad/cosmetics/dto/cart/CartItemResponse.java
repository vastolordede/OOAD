package com.ooad.cosmetics.dto.cart;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long variantId,
        Long productId,
        String productName,
        String sku,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        int stock,
        boolean available,
        String imageUrl
) {
}
