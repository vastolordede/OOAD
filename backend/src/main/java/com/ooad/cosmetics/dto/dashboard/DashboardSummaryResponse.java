package com.ooad.cosmetics.dto.dashboard;

import com.ooad.cosmetics.dto.product.ProductVariantResponse;
import com.ooad.cosmetics.entity.OrderStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardSummaryResponse(
        long totalCustomers,
        long totalProducts,
        long totalOrders,
        BigDecimal totalRevenue,
        Map<OrderStatus, Long> ordersByStatus,
        int lowStockThreshold,
        List<ProductVariantResponse> lowStockVariants
) {
}
