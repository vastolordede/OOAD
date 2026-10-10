package com.ooad.cosmetics.service;

import com.ooad.cosmetics.entity.DiscountType;
import com.ooad.cosmetics.entity.Voucher;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPricingTest {

    @Test
    void shippingFeeIsFlatBelowFreeShippingThreshold() {
        assertThat(OrderPricing.shippingFee(new BigDecimal("499999.99")))
                .isEqualByComparingTo(new BigDecimal("30000"));
    }

    @Test
    void shippingFeeIsFreeAtThreshold() {
        assertThat(OrderPricing.shippingFee(new BigDecimal("500000.00")))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void fixedDiscountReturnsExactValue() {
        Voucher voucher = voucher(DiscountType.FIXED, "50000.00", null);

        assertThat(OrderPricing.discount(voucher, new BigDecimal("200000.00")))
                .isEqualByComparingTo(new BigDecimal("50000"));
    }

    @Test
    void fixedDiscountNeverExceedsSubtotal() {
        Voucher voucher = voucher(DiscountType.FIXED, "300000.00", null);

        assertThat(OrderPricing.discount(voucher, new BigDecimal("200000.00")))
                .isEqualByComparingTo(new BigDecimal("200000"));
    }

    @Test
    void percentageDiscountIsComputedFromSubtotal() {
        Voucher voucher = voucher(DiscountType.PERCENTAGE, "10.00", null);

        assertThat(OrderPricing.discount(voucher, new BigDecimal("250000.00")))
                .isEqualByComparingTo(new BigDecimal("25000"));
    }

    @Test
    void percentageDiscountIsRoundedToTwoDecimals() {
        Voucher voucher = voucher(DiscountType.PERCENTAGE, "15.00", null);

        // 99999.99 * 15 / 100 = 14999.9985 -> 15000.00
        assertThat(OrderPricing.discount(voucher, new BigDecimal("99999.99")))
                .isEqualByComparingTo(new BigDecimal("15000.00"));
    }

    @Test
    void percentageDiscountIsCappedByMaxDiscount() {
        Voucher voucher = voucher(DiscountType.PERCENTAGE, "20.00", "50000.00");

        assertThat(OrderPricing.discount(voucher, new BigDecimal("1000000.00")))
                .isEqualByComparingTo(new BigDecimal("50000"));
    }

    @Test
    void totalIsSubtotalPlusShippingMinusDiscount() {
        BigDecimal total = OrderPricing.total(
                new BigDecimal("200000.00"),
                new BigDecimal("30000.00"),
                new BigDecimal("20000.00")
        );

        assertThat(total).isEqualByComparingTo(new BigDecimal("210000"));
    }

    private Voucher voucher(DiscountType type, String value, String maxDiscount) {
        Voucher voucher = new Voucher();
        voucher.setDiscountType(type);
        voucher.setDiscountValue(new BigDecimal(value));
        voucher.setMaxDiscount(maxDiscount == null ? null : new BigDecimal(maxDiscount));
        return voucher;
    }
}
