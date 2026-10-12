package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.wishlist.AddWishlistItemRequest;
import com.ooad.cosmetics.dto.wishlist.WishlistItemResponse;
import com.ooad.cosmetics.service.WishlistService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@Validated
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ApiResponse<PageResponse<WishlistItemResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(wishlistService.list(page, size));
    }

    @GetMapping("/ids")
    public ApiResponse<List<Long>> productIds() {
        return ApiResponse.success(wishlistService.productIds());
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<WishlistItemResponse>> add(
            @Valid @RequestBody AddWishlistItemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Added to wishlist",
                        wishlistService.add(request.productId())
                ));
    }

    @DeleteMapping("/items/{productId}")
    public ApiResponse<Void> remove(@PathVariable Long productId) {
        wishlistService.remove(productId);
        return ApiResponse.success("Removed from wishlist");
    }
}
