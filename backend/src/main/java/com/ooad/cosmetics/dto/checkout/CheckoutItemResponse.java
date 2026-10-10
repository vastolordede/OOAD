package com.ooad.cosmetics.dto.checkout;

import java.math.BigDecimal;

public record CheckoutItemResponse(
        Long variantId,
        Long productId,
        String productName,
        String sku,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        String imageUrl
) {
}
