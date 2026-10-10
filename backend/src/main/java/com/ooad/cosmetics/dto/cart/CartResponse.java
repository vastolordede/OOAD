package com.ooad.cosmetics.dto.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long id,
        List<CartItemResponse> items,
        int totalQuantity,
        BigDecimal subtotal,
        boolean hasUnavailableItems
) {
}
