package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.catalog.BrandResponse;
import com.ooad.cosmetics.dto.catalog.CatalogNameRequest;
import com.ooad.cosmetics.dto.catalog.CategoryResponse;
import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.service.CatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminCatalogController {

    private final CatalogService catalogService;

    public AdminCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CatalogNameRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Category created",
                        catalogService.createCategory(request)
                ));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CatalogNameRequest request
    ) {
        return ApiResponse.success(
                "Category updated",
                catalogService.updateCategory(id, request)
        );
    }

    @PatchMapping("/categories/{id}/status")
    public ApiResponse<CategoryResponse> categoryStatus(
            @PathVariable Long id,
            @RequestParam CatalogStatus status
    ) {
        return ApiResponse.success(
                "Category status updated",
                catalogService.setCategoryStatus(id, status)
        );
    }

    @GetMapping("/categories")
    public ApiResponse<PageResponse<CategoryResponse>> categories(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(catalogService.adminCategories(page, size));
    }

    @PostMapping("/brands")
    public ResponseEntity<ApiResponse<BrandResponse>> createBrand(
            @Valid @RequestBody CatalogNameRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Brand created",
                        catalogService.createBrand(request)
                ));
    }

    @PutMapping("/brands/{id}")
    public ApiResponse<BrandResponse> updateBrand(
            @PathVariable Long id,
            @Valid @RequestBody CatalogNameRequest request
    ) {
        return ApiResponse.success(
                "Brand updated",
                catalogService.updateBrand(id, request)
        );
    }

    @PatchMapping("/brands/{id}/status")
    public ApiResponse<BrandResponse> brandStatus(
            @PathVariable Long id,
            @RequestParam CatalogStatus status
    ) {
        return ApiResponse.success(
                "Brand status updated",
                catalogService.setBrandStatus(id, status)
        );
    }

    @GetMapping("/brands")
    public ApiResponse<PageResponse<BrandResponse>> brands(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(catalogService.adminBrands(page, size));
    }
}
