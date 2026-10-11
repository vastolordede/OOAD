package com.ooad.cosmetics.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** T15: các ràng buộc ở mức database luôn đúng, kể cả khi code ứng dụng có lỗi. */
@Tag("integration")
class DatabaseConstraintsIntegrationTest extends IntegrationTestBase {

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private void assertRejected(Runnable statement) {
        assertThatThrownBy(statement::run).isInstanceOf(DataIntegrityViolationException.class);
    }

    private long insertOrder(long userId, String subtotal, String shipping, String discount, String total) {
        return jdbc.queryForObject(
                """
                INSERT INTO orders(user_id, receiver_name, receiver_phone, shipping_address_line,
                                   shipping_city, subtotal, shipping_fee, discount_amount, total_amount)
                VALUES (?, 'A', '0900', '1 Le Loi', 'HCM', ?, ?, ?, ?)
                RETURNING id
                """,
                Long.class, userId, bd(subtotal), bd(shipping), bd(discount), bd(total));
    }

    private long insertOrderItem(long orderId, long variantId, String unitPrice, int quantity, String lineTotal) {
        return jdbc.queryForObject(
                """
                INSERT INTO order_items(order_id, variant_id, product_name, sku, unit_price, quantity, line_total)
                VALUES (?, ?, 'Serum', 'S1', ?, ?, ?)
                RETURNING id
                """,
                Long.class, orderId, variantId, bd(unitPrice), quantity, bd(lineTotal));
    }

    // ---------------- kho và giỏ hàng ----------------

    @Test
    void stockAndPriceCanNeverBeNegative() {
        long variantId = createVariant("100000.00", 5);

        assertRejected(() -> jdbc.update("UPDATE product_variants SET stock = -1 WHERE id = ?", variantId));
        assertRejected(() -> jdbc.update("UPDATE product_variants SET price = -1 WHERE id = ?", variantId));
        assertThat(stockOf(variantId)).isEqualTo(5);
    }

    @Test
    void aCustomerCanHaveOnlyOneActiveCart() {
        long userId = createCustomer("buyer@example.com");
        jdbc.update("INSERT INTO carts(user_id) VALUES (?)", userId);

        assertRejected(() -> jdbc.update("INSERT INTO carts(user_id) VALUES (?)", userId));

        // Giỏ không còn ACTIVE thì không bị chặn.
        jdbc.update("INSERT INTO carts(user_id, status) VALUES (?, 'CHECKED_OUT')", userId);
        assertThat(count("SELECT COUNT(*) FROM carts WHERE user_id = ?", userId)).isEqualTo(2);
    }

    @Test
    void aVariantAppearsOnceInACartAndQuantityMustBePositive() {
        long userId = createCustomer("buyer@example.com");
        long cartId = jdbc.queryForObject("INSERT INTO carts(user_id) VALUES (?) RETURNING id", Long.class, userId);
        long variantId = createVariant("100000.00", 5);
        long otherVariantId = createVariant("50000.00", 5);
        jdbc.update("INSERT INTO cart_items(cart_id, variant_id, quantity) VALUES (?, ?, 1)", cartId, variantId);

        assertRejected(() -> jdbc.update(
                "INSERT INTO cart_items(cart_id, variant_id, quantity) VALUES (?, ?, 1)", cartId, variantId));
        assertRejected(() -> jdbc.update(
                "INSERT INTO cart_items(cart_id, variant_id, quantity) VALUES (?, ?, 0)", cartId, otherVariantId));
    }

    // ---------------- voucher ----------------

    @Test
    void voucherRulesAreEnforcedByTheDatabase() {
        String insert = """
                INSERT INTO vouchers(code, discount_type, discount_value, usage_limit, start_date, end_date)
                VALUES (?, ?, ?, ?, now() + (? * interval '1 hour'), now() + (? * interval '1 hour'))
                """;

        jdbc.update(insert, "OK1", "FIXED", bd("1000"), 2, 0, 24);

        assertRejected(() -> jdbc.update(insert, "OK1", "FIXED", bd("1000"), 2, 0, 24));        // mã trùng
        assertRejected(() -> jdbc.update(insert, "BAD1", "FIXED", bd("1000"), 2, 24, 0));       // ngày ngược
        assertRejected(() -> jdbc.update(insert, "BAD2", "FIXED", bd("1000"), 2, 5, 5));        // start = end
        assertRejected(() -> jdbc.update(insert, "BAD3", "PERCENTAGE", bd("101"), 2, 0, 24));   // > 100%
        assertRejected(() -> jdbc.update(insert, "BAD4", "FIXED", bd("0"), 2, 0, 24));          // giá trị 0
        assertRejected(() -> jdbc.update(insert, "BAD5", "FIXED", bd("1000"), 0, 0, 24));       // limit 0
        assertRejected(() -> jdbc.update("UPDATE vouchers SET used_count = 3 WHERE code = 'OK1'"));  // quá limit
    }

    @Test
    void aCustomerCanUseAVoucherOnlyOnce() {
        long userId = createCustomer("buyer@example.com");
        long voucherId = createVoucher("SAVE10", "FIXED", "10000");
        long first = insertOrder(userId, "100000", "30000", "10000", "120000");
        long second = insertOrder(userId, "100000", "30000", "10000", "120000");

        jdbc.update("INSERT INTO voucher_usages(voucher_id, user_id, order_id, discount_amount) VALUES (?, ?, ?, 10000)",
                voucherId, userId, first);

        assertRejected(() -> jdbc.update(
                "INSERT INTO voucher_usages(voucher_id, user_id, order_id, discount_amount) VALUES (?, ?, ?, 10000)",
                voucherId, userId, second));
    }

    // ---------------- đơn hàng ----------------

    @Test
    void orderAmountsMustAddUpAndStatusMustBeKnown() {
        long userId = createCustomer("buyer@example.com");

        assertRejected(() -> insertOrder(userId, "200000", "30000", "0", "999"));      // tổng sai
        assertRejected(() -> insertOrder(userId, "100000", "0", "150000", "0"));       // giảm quá tiền hàng
        assertRejected(() -> insertOrder(userId, "-1", "0", "0", "-1"));               // âm tiền

        long orderId = insertOrder(userId, "200000", "30000", "20000", "210000");
        assertRejected(() -> jdbc.update("UPDATE orders SET status = 'DONE' WHERE id = ?", orderId));
    }

    @Test
    void orderLineTotalMustEqualUnitPriceTimesQuantity() {
        long userId = createCustomer("buyer@example.com");
        long variantId = createVariant("100000.00", 5);
        long orderId = insertOrder(userId, "200000", "30000", "0", "230000");

        assertRejected(() -> insertOrderItem(orderId, variantId, "100000", 2, "5"));
        assertRejected(() -> insertOrderItem(orderId, variantId, "100000", 0, "0"));
        insertOrderItem(orderId, variantId, "100000", 2, "200000");
        assertThat(count("SELECT COUNT(*) FROM order_items")).isEqualTo(1);
    }

    @Test
    void anOrderHasOnePaymentWithAKnownMethodAndStatus() {
        long userId = createCustomer("buyer@example.com");
        long orderId = insertOrder(userId, "100000", "30000", "0", "130000");
        jdbc.update("INSERT INTO payments(order_id, method, amount) VALUES (?, 'COD', 130000)", orderId);

        assertRejected(() -> jdbc.update(
                "INSERT INTO payments(order_id, method, amount) VALUES (?, 'COD', 1)", orderId));
        assertRejected(() -> jdbc.update("UPDATE payments SET method = 'BITCOIN' WHERE order_id = ?", orderId));
        assertRejected(() -> jdbc.update("UPDATE payments SET status = 'LOST' WHERE order_id = ?", orderId));
    }

    @Test
    void aVariantThatIsInAnOrderCannotBeDeletedButDeletingTheOrderCleansUpItsChildren() {
        long userId = createCustomer("buyer@example.com");
        long variantId = createVariant("100000.00", 5);
        long orderId = insertOrder(userId, "100000", "30000", "0", "130000");
        long itemId = insertOrderItem(orderId, variantId, "100000", 1, "100000");
        jdbc.update("INSERT INTO payments(order_id, method, amount) VALUES (?, 'COD', 130000)", orderId);
        jdbc.update("INSERT INTO reviews(order_item_id, user_id, product_id, rating) VALUES (?, ?, ?, 5)",
                itemId, userId, productIdOf(variantId));

        assertRejected(() -> jdbc.update("DELETE FROM product_variants WHERE id = ?", variantId));

        jdbc.update("DELETE FROM orders WHERE id = ?", orderId);
        assertThat(count("SELECT COUNT(*) FROM order_items")).isZero();
        assertThat(count("SELECT COUNT(*) FROM payments")).isZero();
        assertThat(count("SELECT COUNT(*) FROM reviews")).isZero();
    }

    // ---------------- review và wishlist ----------------

    @Test
    void reviewRatingMustBeOneToFiveAndEachOrderItemIsReviewedOnce() {
        long userId = createCustomer("buyer@example.com");
        long variantId = createVariant("100000.00", 5);
        long productId = productIdOf(variantId);
        long orderId = insertOrder(userId, "100000", "30000", "0", "130000");
        long itemId = insertOrderItem(orderId, variantId, "100000", 1, "100000");

        String insert = "INSERT INTO reviews(order_item_id, user_id, product_id, rating) VALUES (?, ?, ?, ?)";
        assertRejected(() -> jdbc.update(insert, itemId, userId, productId, 0));
        assertRejected(() -> jdbc.update(insert, itemId, userId, productId, 6));

        jdbc.update(insert, itemId, userId, productId, 5);
        assertRejected(() -> jdbc.update(insert, itemId, userId, productId, 4));
        assertRejected(() -> jdbc.update("UPDATE reviews SET status = 'DELETED'"));
    }

    @Test
    void aWishlistHoldsEachProductOnceAndEachCustomerHasOneWishlist() {
        long userId = createCustomer("buyer@example.com");
        long productId = productIdOf(createVariant("100000.00", 5));
        long wishlistId = jdbc.queryForObject(
                "INSERT INTO wishlists(user_id) VALUES (?) RETURNING id", Long.class, userId);
        jdbc.update("INSERT INTO wishlist_items(wishlist_id, product_id) VALUES (?, ?)", wishlistId, productId);

        assertRejected(() -> jdbc.update("INSERT INTO wishlists(user_id) VALUES (?)", userId));
        assertRejected(() -> jdbc.update(
                "INSERT INTO wishlist_items(wishlist_id, product_id) VALUES (?, ?)", wishlistId, productId));
    }
}
