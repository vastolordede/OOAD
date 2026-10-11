package com.ooad.cosmetics.dto.dashboard;

import java.math.BigDecimal;

public record TopProductResponse(
        Long productId,
        String productName,
        long quantitySold,
        BigDecimal revenue
) {
}
