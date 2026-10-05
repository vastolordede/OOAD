package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.product.*;
import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.service.ProductService;
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
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Product created",
                        productService.createProduct(request)
                ));
    }

    @PutMapping("/products/{id}")
    public ApiResponse<ProductDetailResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return ApiResponse.success(
                "Product updated",
                productService.updateProduct(id, request)
        );
    }

    @PatchMapping("/products/{id}/status")
    public ApiResponse<ProductDetailResponse> productStatus(
            @PathVariable Long id,
            @RequestParam CatalogStatus status
    ) {
        return ApiResponse.success(
                "Product status updated",
                productService.setProductStatus(id, status)
        );
    }

    @GetMapping("/products")
    public ApiResponse<PageResponse<ProductSummaryResponse>> products(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CatalogStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(
                productService.adminProducts(q, status, page, size)
        );
    }

    @PostMapping("/products/{productId}/variants")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> createVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Variant created",
                        productService.createVariant(productId, request)
                ));
    }

    @PutMapping("/variants/{id}")
    public ApiResponse<ProductVariantResponse> updateVariant(
            @PathVariable Long id,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        return ApiResponse.success(
                "Variant updated",
                productService.updateVariant(id, request)
        );
    }

    @PatchMapping("/variants/{id}/status")
    public ApiResponse<ProductVariantResponse> variantStatus(
            @PathVariable Long id,
            @RequestParam CatalogStatus status
    ) {
        return ApiResponse.success(
                "Variant status updated",
                productService.setVariantStatus(id, status)
        );
    }

    @PostMapping("/products/{productId}/images")
    public ResponseEntity<ApiResponse<ProductImageResponse>> addImage(
            @PathVariable Long productId,
            @Valid @RequestBody ProductImageRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Product image added",
                        productService.addImage(productId, request)
                ));
    }

    @DeleteMapping("/products/{productId}/images/{imageId}")
    public ApiResponse<Void> removeImage(
            @PathVariable Long productId,
            @PathVariable Long imageId
    ) {
        productService.removeImage(productId, imageId);
        return ApiResponse.success("Product image removed");
    }

    @PutMapping("/products/{productId}/images/{imageId}/primary")
    public ApiResponse<ProductImageResponse> setPrimaryImage(
            @PathVariable Long productId,
            @PathVariable Long imageId
    ) {
        return ApiResponse.success(
                "Primary image updated",
                productService.setPrimaryImage(productId, imageId)
        );
    }
}
