package com.ooad.cosmetics.dto.voucher;

import com.ooad.cosmetics.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record VoucherRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "must contain only letters, digits, '_' or '-'")
        String code,

        @Size(max = 255)
        String description,

        @NotNull
        DiscountType discountType,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal discountValue,

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal minOrderValue,

        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal maxDiscount,

        @Min(1)
        Integer usageLimit,

        @NotNull
        Instant startDate,

        @NotNull
        Instant endDate
) {
}
