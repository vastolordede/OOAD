package com.ooad.cosmetics.dto.wishlist;

import jakarta.validation.constraints.NotNull;

public record AddWishlistItemRequest(
        @NotNull
        Long productId
) {
}
