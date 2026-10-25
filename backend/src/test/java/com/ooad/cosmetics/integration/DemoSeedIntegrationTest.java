package com.ooad.cosmetics.integration;

import com.ooad.cosmetics.service.DashboardService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest(properties = {
        "spring.datasource.url=${DEMO_TEST_DB_URL:jdbc:postgresql://localhost:5432/ooad_cosmetics_demo_test}",
        "spring.flyway.locations=classpath:db/migration,classpath:db/seed"
})
@ActiveProfiles({"test", "demo"})
class DemoSeedIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DashboardService dashboardService;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Test
    void demoFlywaySeedHasConsistentOrdersInventoryAndDashboardData() {
        assertThat(datasourceUrl)
                .as("Demo seed integration test must use a dedicated database")
                .containsIgnoringCase("test");
        assertThat(count("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '100' AND success"))
                .isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM flyway_schema_history WHERE success = FALSE"))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE"))
                .isEqualTo(10);
        assertThat(count("SELECT COUNT(*) FROM users")).isEqualTo(4);
        assertThat(count("SELECT COUNT(*) FROM addresses")).isEqualTo(4);
        assertThat(count("SELECT COUNT(*) FROM vouchers")).isEqualTo(6);
        assertThat(count("SELECT COUNT(*) FROM vouchers WHERE status = 'ACTIVE'")).isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM vouchers WHERE status = 'INACTIVE'")).isEqualTo(1);
        assertThat(count("""
                SELECT COUNT(*) FROM vouchers
                WHERE code = 'DEMO-FULL' AND used_count = usage_limit
                """)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM orders")).isEqualTo(17);
        assertThat(count("SELECT COUNT(DISTINCT status) FROM orders")).isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM orders WHERE status = 'DELIVERED'")).isEqualTo(12);
        assertThat(count("SELECT COUNT(*) FROM orders WHERE status = 'CANCELLED'")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM order_items")).isEqualTo(17);
        assertThat(count("SELECT COUNT(*) FROM payments")).isEqualTo(17);
        assertThat(count("SELECT COUNT(*) FROM payments WHERE status = 'PAID'")).isGreaterThan(0);
        assertThat(count("SELECT COUNT(*) FROM payments WHERE status = 'PENDING'")).isGreaterThan(0);
        assertThat(count("SELECT COUNT(*) FROM payments WHERE status = 'FAILED'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM payments WHERE status = 'REFUNDED'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM voucher_usages")).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM reviews")).isEqualTo(12);
        assertThat(count("SELECT COUNT(*) FROM reviews WHERE status = 'HIDDEN'")).isEqualTo(2);
        assertThat(count("SELECT COUNT(DISTINCT rating) FROM reviews")).isEqualTo(5);
        assertThat(count("""
                SELECT COUNT(*) FROM orders
                WHERE status = 'DELIVERED'
                  AND delivered_at >= CURRENT_TIMESTAMP - INTERVAL '30 days'
                """)).isEqualTo(12);
        assertThat(count("""
                SELECT COUNT(*) FROM product_variants
                WHERE sku IN ('DEMO-GLOW-30', 'DEMO-HYDRATE-30') AND stock <= 10
                """)).isEqualTo(2);

        assertThat(count("""
                SELECT COUNT(*) FROM orders
                WHERE total_amount <> subtotal + shipping_fee - discount_amount
                   OR discount_amount > subtotal
                   OR shipping_fee <> CASE WHEN subtotal < 500000 THEN 30000 ELSE 0 END
                   OR (status = 'DELIVERED' AND delivered_at IS NULL)
                   OR (status <> 'DELIVERED' AND delivered_at IS NOT NULL)
                   OR (status = 'CANCELLED' AND cancelled_at IS NULL)
                   OR (status <> 'CANCELLED' AND cancelled_at IS NOT NULL)
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM order_items
                JOIN product_variants v ON v.id = order_items.variant_id
                JOIN products p ON p.id = v.product_id
                WHERE line_total <> unit_price * quantity
                   OR sku <> v.sku
                   OR product_name <> p.name
                   OR unit_price <> v.price
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM (
                    SELECT v.sku
                    FROM product_variants v
                    JOIN (VALUES ('DEMO-GLOW-30', 15), ('DEMO-HYDRATE-30', 7),
                                 ('DEMO-NIGHT-50', 20)) stock(sku, initial_stock)
                      ON stock.sku = v.sku
                    LEFT JOIN order_items oi ON oi.variant_id = v.id
                    LEFT JOIN orders o ON o.id = oi.order_id AND o.status <> 'CANCELLED'
                    GROUP BY v.sku, stock.initial_stock, v.stock
                    HAVING v.stock <> stock.initial_stock - COALESCE(SUM(oi.quantity)
                        FILTER (WHERE o.id IS NOT NULL), 0)
                ) inconsistent_stock
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM orders o
                LEFT JOIN voucher_usages vu ON vu.order_id = o.id
                WHERE (o.voucher_id IS NOT NULL AND (vu.id IS NULL
                       OR vu.voucher_id <> o.voucher_id OR vu.user_id <> o.user_id))
                   OR (o.voucher_id IS NULL AND vu.id IS NOT NULL)
                   OR (o.status = 'CANCELLED' AND vu.id IS NOT NULL)
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM vouchers v
                WHERE v.used_count <> (SELECT COUNT(*) FROM voucher_usages vu WHERE vu.voucher_id = v.id)
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM orders o
                JOIN order_items oi ON oi.order_id = o.id
                LEFT JOIN reviews r ON r.order_item_id = oi.id
                WHERE o.status = 'DELIVERED' AND r.id IS NULL
                """)).isZero();

        BigDecimal revenue = dashboardService.summary(10).totalRevenue();
        assertThat(revenue).isGreaterThan(BigDecimal.ZERO);
    }

    private long count(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }
}
