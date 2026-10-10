package com.ooad.cosmetics.service;

import com.ooad.cosmetics.entity.DiscountType;
import com.ooad.cosmetics.entity.Voucher;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Quy tắc tính tiền của đơn hàng (O09 - O12), viết thành hàm thuần để dễ test.
 * Mọi số tiền đều ở scale 2 để khớp CHECK (total = subtotal + shipping - discount) ở database.
 */
public final class OrderPricing {

    /** Phí ship cố định: 30.000 VND. */
    public static final BigDecimal FLAT_SHIPPING_FEE = new BigDecimal("30000.00");

    /** Miễn phí ship khi tạm tính từ 500.000 VND trở lên. */
    public static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500000.00");

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private OrderPricing() {
    }

    /** O10 */
    public static BigDecimal shippingFee(BigDecimal subtotal) {
        return subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? ZERO : FLAT_SHIPPING_FEE;
    }

    /**
     * O11, V09, V10, V11.
     * FIXED: giảm đúng discountValue.
     * PERCENTAGE: giảm subtotal * % / 100, không vượt maxDiscount (nếu có).
     * Cả hai đều không vượt subtotal.
     */
    public static BigDecimal discount(Voucher voucher, BigDecimal subtotal) {
        BigDecimal discount;

        if (voucher.getDiscountType() == DiscountType.FIXED) {
            discount = voucher.getDiscountValue();
        } else {
            discount = subtotal
                    .multiply(voucher.getDiscountValue())
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);

            BigDecimal max = voucher.getMaxDiscount();
            if (max != null && discount.compareTo(max) > 0) {
                discount = max;
            }
        }

        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }

        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    /** O12 */
    public static BigDecimal total(BigDecimal subtotal, BigDecimal shippingFee, BigDecimal discount) {
        return subtotal.add(shippingFee).subtract(discount).setScale(2, RoundingMode.HALF_UP);
    }
}
