package com.ooad.cosmetics.dto.order;

import com.ooad.cosmetics.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotNull
        Long addressId,

        @Size(max = 50)
        String voucherCode,

        @NotNull
        PaymentMethod paymentMethod,

        @Size(max = 500)
        String note
) {
}
