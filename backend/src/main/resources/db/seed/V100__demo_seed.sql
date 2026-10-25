-- TẠM - thay bằng seed của Đặng
-- Demo-only data. References use stable emails, names, and SKUs rather than generated IDs.

INSERT INTO users (email, password_hash, full_name, phone, role)
VALUES
    ('admin@ooad.demo', '$2a$10$Y3LViyvjhjzq9348RqkIbuy23vZsOWcxObNLjvLISLV7yEf8lCm0a', 'Demo Administrator', '0900000001', 'ADMIN'),
    ('anh@ooad.demo', '$2a$10$Y3LViyvjhjzq9348RqkIbuy23vZsOWcxObNLjvLISLV7yEf8lCm0a', 'Demo Anh', '0900000002', 'CUSTOMER'),
    ('minh@ooad.demo', '$2a$10$Y3LViyvjhjzq9348RqkIbuy23vZsOWcxObNLjvLISLV7yEf8lCm0a', 'Demo Minh', '0900000003', 'CUSTOMER'),
    ('linh@ooad.demo', '$2a$10$Y3LViyvjhjzq9348RqkIbuy23vZsOWcxObNLjvLISLV7yEf8lCm0a', 'Demo Linh', '0900000004', 'CUSTOMER');

INSERT INTO addresses (user_id, receiver_name, phone, address_line, ward, district, city, is_default)
SELECT id, 'Demo Anh', '0900000002', '1 Demo Street', 'Ward 1', 'District 1', 'Ho Chi Minh City', TRUE
FROM users WHERE email = 'anh@ooad.demo';
INSERT INTO addresses (user_id, receiver_name, phone, address_line, ward, district, city, is_default)
SELECT id, 'Demo Anh', '0900000002', '2 Demo Street', 'Ward 2', 'District 3', 'Ho Chi Minh City', FALSE
FROM users WHERE email = 'anh@ooad.demo';
INSERT INTO addresses (user_id, receiver_name, phone, address_line, ward, district, city, is_default)
SELECT id, 'Demo Minh', '0900000003', '3 Demo Street', 'Ward 3', 'District 5', 'Ho Chi Minh City', TRUE
FROM users WHERE email = 'minh@ooad.demo';
INSERT INTO addresses (user_id, receiver_name, phone, address_line, ward, district, city, is_default)
SELECT id, 'Demo Linh', '0900000004', '4 Demo Street', 'Ward 4', 'District 7', 'Ho Chi Minh City', TRUE
FROM users WHERE email = 'linh@ooad.demo';

INSERT INTO categories (name) VALUES ('Demo Skin Care');
INSERT INTO brands (name) VALUES ('OOAD Demo Studio');

INSERT INTO products (category_id, brand_id, name, description)
SELECT c.id, b.id, 'Demo Glow Serum', 'Temporary demo product; replace with Đặng seed.'
FROM categories c CROSS JOIN brands b
WHERE c.name = 'Demo Skin Care' AND b.name = 'OOAD Demo Studio';
INSERT INTO products (category_id, brand_id, name, description)
SELECT c.id, b.id, 'Demo Hydrating Serum', 'Temporary demo product; replace with Đặng seed.'
FROM categories c CROSS JOIN brands b
WHERE c.name = 'Demo Skin Care' AND b.name = 'OOAD Demo Studio';
INSERT INTO products (category_id, brand_id, name, description)
SELECT c.id, b.id, 'Demo Night Cream', 'Temporary demo product; replace with Đặng seed.'
FROM categories c CROSS JOIN brands b
WHERE c.name = 'Demo Skin Care' AND b.name = 'OOAD Demo Studio';

INSERT INTO product_variants (product_id, sku, price, stock)
SELECT id, 'DEMO-GLOW-30', 500000.00, 8 FROM products WHERE name = 'Demo Glow Serum';
INSERT INTO product_variants (product_id, sku, price, stock)
SELECT id, 'DEMO-HYDRATE-30', 200000.00, 2 FROM products WHERE name = 'Demo Hydrating Serum';
INSERT INTO product_variants (product_id, sku, price, stock)
SELECT id, 'DEMO-NIGHT-50', 250000.00, 17 FROM products WHERE name = 'Demo Night Cream';

INSERT INTO vouchers (
    code, description, discount_type, discount_value, min_order_value,
    max_discount, usage_limit, used_count, start_date, end_date, status
)
VALUES
    ('DEMO-PERCENT', 'Temporary demo percentage voucher', 'PERCENTAGE', 10, 0, 30000, NULL, 1,
     CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '30 days', 'ACTIVE'),
    ('DEMO-FIXED', 'Temporary demo fixed voucher', 'FIXED', 15000, 0, NULL, NULL, 1,
     CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '30 days', 'ACTIVE'),
    ('DEMO-EXPIRED', 'Temporary demo expired voucher', 'FIXED', 10000, 0, NULL, NULL, 0,
     CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '1 day', 'ACTIVE'),
    ('DEMO-FUTURE', 'Temporary demo not-yet-valid voucher', 'FIXED', 10000, 0, NULL, NULL, 0,
     CURRENT_TIMESTAMP + INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '30 days', 'ACTIVE'),
    ('DEMO-INACTIVE', 'Temporary demo inactive voucher', 'FIXED', 10000, 0, NULL, NULL, 0,
     CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '30 days', 'INACTIVE'),
    ('DEMO-FULL', 'Temporary demo fully used voucher', 'FIXED', 10000, 0, NULL, 1, 1,
     CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '30 days', 'ACTIVE');

WITH seed_orders (
    order_key, email, sku, order_status, quantity, days_ago, voucher_code, discount_amount
) AS (
    VALUES
        ('DEMO-D01', 'anh@ooad.demo',  'DEMO-GLOW-30',     'DELIVERED', 1,  1, 'DEMO-PERCENT', 30000.00),
        ('DEMO-D02', 'minh@ooad.demo', 'DEMO-GLOW-30',     'DELIVERED', 1,  3, NULL,               0.00),
        ('DEMO-D03', 'linh@ooad.demo', 'DEMO-GLOW-30',     'DELIVERED', 1,  5, NULL,               0.00),
        ('DEMO-D04', 'anh@ooad.demo',  'DEMO-GLOW-30',     'DELIVERED', 1,  7, NULL,               0.00),
        ('DEMO-D05', 'minh@ooad.demo', 'DEMO-GLOW-30',     'DELIVERED', 1,  9, NULL,               0.00),
        ('DEMO-D06', 'minh@ooad.demo', 'DEMO-HYDRATE-30',  'DELIVERED', 1, 11, 'DEMO-FIXED',   15000.00),
        ('DEMO-D07', 'linh@ooad.demo', 'DEMO-HYDRATE-30',  'DELIVERED', 1, 13, NULL,               0.00),
        ('DEMO-D08', 'anh@ooad.demo',  'DEMO-HYDRATE-30',  'DELIVERED', 1, 15, NULL,               0.00),
        ('DEMO-D09', 'minh@ooad.demo', 'DEMO-HYDRATE-30',  'DELIVERED', 1, 17, NULL,               0.00),
        ('DEMO-D10', 'linh@ooad.demo', 'DEMO-NIGHT-50',    'DELIVERED', 1, 19, NULL,               0.00),
        ('DEMO-D11', 'anh@ooad.demo',  'DEMO-NIGHT-50',    'DELIVERED', 1, 21, NULL,               0.00),
        ('DEMO-D12', 'linh@ooad.demo', 'DEMO-NIGHT-50',    'DELIVERED', 1, 23, 'DEMO-FULL',   10000.00),
        ('DEMO-PENDING',   'anh@ooad.demo',  'DEMO-GLOW-30',    'PENDING',   1,  0, NULL, 0.00),
        ('DEMO-CONFIRMED', 'minh@ooad.demo', 'DEMO-GLOW-30',    'CONFIRMED', 1,  1, NULL, 0.00),
        ('DEMO-SHIPPING',  'linh@ooad.demo', 'DEMO-HYDRATE-30', 'SHIPPING',  1,  2, NULL, 0.00),
        ('DEMO-CANCELLED-FAILED', 'anh@ooad.demo',  'DEMO-NIGHT-50', 'CANCELLED', 2, 4, NULL, 0.00),
        ('DEMO-CANCELLED-REFUNDED', 'minh@ooad.demo', 'DEMO-NIGHT-50', 'CANCELLED', 1, 6, NULL, 0.00)
)
INSERT INTO orders (
    user_id, status, receiver_name, receiver_phone, shipping_address_line,
    shipping_ward, shipping_district, shipping_city, subtotal, shipping_fee,
    discount_amount, total_amount, voucher_id, voucher_code, note,
    delivered_at, cancelled_at, created_at
)
SELECT
    u.id,
    s.order_status,
    u.full_name,
    u.phone,
    '1 Demo Street',
    'Ward 1',
    'District 1',
    'Ho Chi Minh City',
    v.price * s.quantity,
    CASE WHEN v.price * s.quantity < 500000 THEN 30000.00 ELSE 0.00 END,
    s.discount_amount,
    v.price * s.quantity
        + CASE WHEN v.price * s.quantity < 500000 THEN 30000.00 ELSE 0.00 END
        - s.discount_amount,
    voucher.id,
    s.voucher_code,
    s.order_key,
    CASE WHEN s.order_status = 'DELIVERED'
         THEN CURRENT_TIMESTAMP - s.days_ago * INTERVAL '1 day' + INTERVAL '1 hour' END,
    CASE WHEN s.order_status = 'CANCELLED'
         THEN CURRENT_TIMESTAMP - s.days_ago * INTERVAL '1 day' END,
    CURRENT_TIMESTAMP - s.days_ago * INTERVAL '1 day'
FROM seed_orders s
JOIN users u ON u.email = s.email
JOIN product_variants v ON v.sku = s.sku
LEFT JOIN vouchers voucher ON voucher.code = s.voucher_code;

INSERT INTO order_items (order_id, variant_id, product_name, sku, unit_price, quantity, line_total)
SELECT o.id, v.id, p.name, v.sku,
       v.price,
       CASE WHEN o.note = 'DEMO-CANCELLED-FAILED' THEN 2 ELSE 1 END,
       v.price * CASE WHEN o.note = 'DEMO-CANCELLED-FAILED' THEN 2 ELSE 1 END
FROM orders o
JOIN product_variants v ON v.sku = CASE
    WHEN o.note IN ('DEMO-D01', 'DEMO-D02', 'DEMO-D03', 'DEMO-D04', 'DEMO-D05',
                    'DEMO-PENDING', 'DEMO-CONFIRMED') THEN 'DEMO-GLOW-30'
    WHEN o.note IN ('DEMO-D06', 'DEMO-D07', 'DEMO-D08', 'DEMO-D09', 'DEMO-SHIPPING')
        THEN 'DEMO-HYDRATE-30'
    ELSE 'DEMO-NIGHT-50'
END
JOIN products p ON p.id = v.product_id
WHERE o.note LIKE 'DEMO-%';

INSERT INTO payments (order_id, method, status, amount, transaction_ref, paid_at)
SELECT o.id,
       CASE WHEN o.note IN ('DEMO-D01', 'DEMO-D03', 'DEMO-D05', 'DEMO-D07',
                            'DEMO-D09', 'DEMO-D11', 'DEMO-CONFIRMED', 'DEMO-PENDING',
                            'DEMO-CANCELLED-FAILED', 'DEMO-CANCELLED-REFUNDED')
            THEN 'ONLINE' ELSE 'COD' END,
       CASE
           WHEN o.status IN ('DELIVERED', 'CONFIRMED') THEN 'PAID'
           WHEN o.note = 'DEMO-CANCELLED-REFUNDED' THEN 'REFUNDED'
           WHEN o.note = 'DEMO-CANCELLED-FAILED' THEN 'FAILED'
           ELSE 'PENDING'
       END,
       o.total_amount,
       CASE WHEN o.note IN ('DEMO-D01', 'DEMO-D03', 'DEMO-D05', 'DEMO-D07',
                            'DEMO-D09', 'DEMO-D11', 'DEMO-CONFIRMED', 'DEMO-PENDING',
                            'DEMO-CANCELLED-FAILED', 'DEMO-CANCELLED-REFUNDED')
            THEN 'DEMO-TXN-' || o.note END,
       CASE WHEN o.status IN ('DELIVERED', 'CONFIRMED')
                  OR o.note = 'DEMO-CANCELLED-REFUNDED'
            THEN o.created_at + INTERVAL '1 hour' END
FROM orders o
WHERE o.note LIKE 'DEMO-%';

INSERT INTO voucher_usages (voucher_id, user_id, order_id, discount_amount)
SELECT o.voucher_id, o.user_id, o.id, o.discount_amount
FROM orders o
WHERE o.voucher_id IS NOT NULL
  AND o.status <> 'CANCELLED';

INSERT INTO reviews (order_item_id, user_id, product_id, rating, comment, status)
SELECT oi.id,
       o.user_id,
       v.product_id,
       CASE o.note
           WHEN 'DEMO-D01' THEN 1
           WHEN 'DEMO-D02' THEN 2
           WHEN 'DEMO-D03' THEN 3
           WHEN 'DEMO-D04' THEN 4
           WHEN 'DEMO-D05' THEN 5
           WHEN 'DEMO-D06' THEN 1
           WHEN 'DEMO-D07' THEN 2
           WHEN 'DEMO-D08' THEN 3
           WHEN 'DEMO-D09' THEN 4
           WHEN 'DEMO-D10' THEN 5
           WHEN 'DEMO-D11' THEN 3
           ELSE 4
       END,
       'Temporary demo review ' || o.note,
       CASE WHEN o.note IN ('DEMO-D04', 'DEMO-D09') THEN 'HIDDEN' ELSE 'VISIBLE' END
FROM orders o
JOIN order_items oi ON oi.order_id = o.id
JOIN product_variants v ON v.id = oi.variant_id
WHERE o.status = 'DELIVERED' AND o.note LIKE 'DEMO-D%';
