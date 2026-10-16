package com.ooad.cosmetics.dto.voucher;

import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.entity.DiscountType;
import com.ooad.cosmetics.entity.Voucher;

import java.math.BigDecimal;
import java.time.Instant;

public record VoucherResponse(
        Long id,
        String code,
        String description,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minOrderValue,
        BigDecimal maxDiscount,
        Integer usageLimit,
        int usedCount,
        Instant startDate,
        Instant endDate,
        CatalogStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static VoucherResponse from(Voucher voucher) {
        return new VoucherResponse(
                voucher.getId(),
                voucher.getCode(),
                voucher.getDescription(),
                voucher.getDiscountType(),
                voucher.getDiscountValue(),
                voucher.getMinOrderValue(),
                voucher.getMaxDiscount(),
                voucher.getUsageLimit(),
                voucher.getUsedCount(),
                voucher.getStartDate(),
                voucher.getEndDate(),
                voucher.getStatus(),
                voucher.getCreatedAt(),
                voucher.getUpdatedAt()
        );
    }
}
