package com.ooad.cosmetics.integration;

import com.ooad.cosmetics.dto.cart.AddCartItemRequest;
import com.ooad.cosmetics.service.CartService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nền chung cho integration test: Spring Boot thật + PostgreSQL thật (database test riêng).
 * Mỗi test bắt đầu với database trống (các bảng đã được Flyway tạo sẵn).
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final String PASSWORD = "Password123";
    private static final AtomicLong SEQUENCE = new AtomicLong();

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected CartService cartService;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @BeforeEach
    protected void resetDatabase() {
        // Chốt an toàn: tuyệt đối không xóa dữ liệu nếu đây không phải database test.
        assertThat(datasourceUrl)
                .as("Integration tests must run on a dedicated database whose name contains 'test'")
                .containsIgnoringCase("test");

        jdbc.execute("""
                TRUNCATE TABLE reviews, voucher_usages, payments, order_items, orders,
                               cart_items, carts, wishlist_items, wishlists, vouchers,
                               product_images, product_variants, products, brands, categories,
                               password_reset_tokens, addresses, users
                RESTART IDENTITY CASCADE
                """);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    protected void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // Dữ liệu mẫu (ghi thẳng bằng SQL để không phụ thuộc vào code đang được test)
    // ------------------------------------------------------------------

    protected long createUser(String email, String role) {
        return jdbc.queryForObject(
                "INSERT INTO users(email, password_hash, full_name, role) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                email,
                passwordEncoder.encode(PASSWORD),
                "User " + email,
                role
        );
    }

    protected long createCustomer(String email) {
        return createUser(email, "CUSTOMER");
    }

    protected long createAdmin(String email) {
        return createUser(email, "ADMIN");
    }

    protected long createAddress(long userId) {
        return jdbc.queryForObject(
                """
                INSERT INTO addresses(user_id, receiver_name, phone, address_line, city, is_default)
                VALUES (?, 'Nguyen Van A', '0900000000', '1 Le Loi', 'Ho Chi Minh', TRUE)
                RETURNING id
                """,
                Long.class,
                userId
        );
    }

    /** Tạo mới category, brand, product và một variant ACTIVE; trả về id của variant. */
    protected long createVariant(String price, int stock) {
        long n = SEQUENCE.incrementAndGet();
        Long category = jdbc.queryForObject(
                "INSERT INTO categories(name) VALUES (?) RETURNING id", Long.class, "Category " + n);
        Long brand = jdbc.queryForObject(
                "INSERT INTO brands(name) VALUES (?) RETURNING id", Long.class, "Brand " + n);
        Long product = jdbc.queryForObject(
                "INSERT INTO products(category_id, brand_id, name) VALUES (?, ?, ?) RETURNING id",
                Long.class, category, brand, "Product " + n);
        return jdbc.queryForObject(
                "INSERT INTO product_variants(product_id, sku, price, stock) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, product, "SKU-" + n, new BigDecimal(price), stock);
    }

    protected long productIdOf(long variantId) {
        return jdbc.queryForObject(
                "SELECT product_id FROM product_variants WHERE id = ?", Long.class, variantId);
    }

    protected long createVoucher(String code, String type, String value) {
        return createVoucher(code, type, value, "0", null, null, -1, 24);
    }

    protected long createVoucher(
            String code,
            String type,
            String value,
            String minOrderValue,
            String maxDiscount,
            Integer usageLimit,
            long startHoursFromNow,
            long endHoursFromNow
    ) {
        Instant now = Instant.now();
        return jdbc.queryForObject(
                """
                INSERT INTO vouchers(code, discount_type, discount_value, min_order_value,
                                     max_discount, usage_limit, start_date, end_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                code,
                type,
                new BigDecimal(value),
                new BigDecimal(minOrderValue),
                maxDiscount == null ? null : new BigDecimal(maxDiscount),
                usageLimit,
                OffsetDateTime.ofInstant(now.plus(startHoursFromNow, ChronoUnit.HOURS), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(now.plus(endHoursFromNow, ChronoUnit.HOURS), ZoneOffset.UTC)
        );
    }

    /**
     * Tạo khách hàng, địa chỉ và đưa sản phẩm vào giỏ (qua CartService thật).
     * Sau khi gọi, luồng hiện tại đang đăng nhập với tư cách khách đó. Trả về id địa chỉ.
     */
    protected long prepareCustomerWithCart(String email, long variantId, int quantity) {
        long userId = createCustomer(email);
        long addressId = createAddress(userId);
        authenticateAs(email, "CUSTOMER");
        cartService.addItem(new AddCartItemRequest(variantId, quantity));
        return addressId;
    }

    protected void authenticateAs(String email, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                )
        );
    }

    // ------------------------------------------------------------------
    // Truy vấn kiểm tra
    // ------------------------------------------------------------------

    protected int stockOf(long variantId) {
        return jdbc.queryForObject(
                "SELECT stock FROM product_variants WHERE id = ?", Integer.class, variantId);
    }

    protected long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    protected String orderStatus(long orderId) {
        return jdbc.queryForObject(
                "SELECT status FROM orders WHERE id = ?", String.class, orderId);
    }

    // ------------------------------------------------------------------
    // Chạy nhiều tác vụ đồng thời, bắt đầu cùng một lúc
    // ------------------------------------------------------------------

    /** Trả về kết quả hoặc Exception của từng tác vụ, theo đúng thứ tự đầu vào. */
    protected <T> List<Object> runConcurrently(List<Callable<T>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch go = new CountDownLatch(1);

        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        return task.call();
                    } catch (Exception ex) {
                        return ex;
                    }
                }));
            }

            ready.await();
            go.countDown();

            List<Object> outcomes = new ArrayList<>();
            for (Future<Object> future : futures) {
                outcomes.add(future.get(60, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }
}
