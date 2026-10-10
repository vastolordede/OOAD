package com.ooad.cosmetics.dto.checkout;

import com.ooad.cosmetics.dto.address.AddressResponse;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutPreviewResponse(
        List<CheckoutItemResponse> items,
        AddressResponse shippingAddress,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String voucherCode
) {
}
