package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.dto.cart.AddCartItemRequest;
import com.ooad.cosmetics.dto.cart.CartResponse;
import com.ooad.cosmetics.dto.cart.UpdateCartItemRequest;
import com.ooad.cosmetics.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<CartResponse> getCart() {
        return ApiResponse.success(cartService.getMyCart());
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Item added to cart",
                        cartService.addItem(request)
                ));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<CartResponse> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return ApiResponse.success(
                "Cart item updated",
                cartService.updateItem(itemId, request)
        );
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartResponse> removeItem(@PathVariable Long itemId) {
        return ApiResponse.success(
                "Cart item removed",
                cartService.removeItem(itemId)
        );
    }

    @DeleteMapping
    public ApiResponse<CartResponse> clear() {
        return ApiResponse.success("Cart cleared", cartService.clear());
    }
}
