package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.product.ProductVariantResponse;
import com.ooad.cosmetics.dto.product.StockUpdateRequest;
import com.ooad.cosmetics.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/inventory")
@Validated
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PutMapping("/variants/{variantId}/stock")
    public ApiResponse<ProductVariantResponse> updateStock(
            @PathVariable Long variantId,
            @Valid @RequestBody StockUpdateRequest request
    ) {
        return ApiResponse.success(
                "Stock updated",
                inventoryService.updateStock(variantId, request)
        );
    }

    @GetMapping("/low-stock")
    @Operation(summary = "List active product variants at or below the stock threshold")
    public ApiResponse<PageResponse<ProductVariantResponse>> lowStock(
            @Parameter(description = "Maximum stock quantity to include (inclusive)")
            @RequestParam(defaultValue = "10") @Min(0) int threshold,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (1-100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(
                inventoryService.lowStock(threshold, page, size)
        );
    }
}
