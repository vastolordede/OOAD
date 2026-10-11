package com.ooad.cosmetics.dto.dashboard;

import java.math.BigDecimal;

public record RevenuePointResponse(
        String period,
        BigDecimal revenue,
        long orderCount
) {
}
