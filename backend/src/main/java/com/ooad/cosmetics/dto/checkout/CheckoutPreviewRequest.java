package com.ooad.cosmetics.dto.checkout;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckoutPreviewRequest(
        @NotNull
        Long addressId,

        @Size(max = 50)
        String voucherCode
) {
}
