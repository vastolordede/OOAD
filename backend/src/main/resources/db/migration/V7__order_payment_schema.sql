CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- O08: snapshot người nhận / địa chỉ giao hàng tại thời điểm đặt.
    receiver_name VARCHAR(150) NOT NULL,
    receiver_phone VARCHAR(30) NOT NULL,
    shipping_address_line VARCHAR(255) NOT NULL,
    shipping_ward VARCHAR(120),
    shipping_district VARCHAR(120),
    shipping_city VARCHAR(120) NOT NULL,

    subtotal NUMERIC(14, 2) NOT NULL,
    shipping_fee NUMERIC(14, 2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(14, 2) NOT NULL,

    voucher_id BIGINT,
    voucher_code VARCHAR(50),
    note VARCHAR(500),
    delivered_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_orders_voucher
        FOREIGN KEY (voucher_id) REFERENCES vouchers(id),
    CONSTRAINT chk_orders_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPING', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT chk_orders_amounts
        CHECK (subtotal >= 0 AND shipping_fee >= 0 AND discount_amount >= 0 AND total_amount >= 0),
    CONSTRAINT chk_orders_discount_not_over_subtotal
        CHECK (discount_amount <= subtotal),
    -- O12
    CONSTRAINT chk_orders_total
        CHECK (total_amount = subtotal + shipping_fee - discount_amount)
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_voucher_id ON orders(voucher_id);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,

    -- O16: snapshot tên / SKU / đơn giá tại thời điểm đặt.
    product_name VARCHAR(200) NOT NULL,
    sku VARCHAR(100) NOT NULL,
    unit_price NUMERIC(14, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    line_total NUMERIC(14, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_variant
        FOREIGN KEY (variant_id) REFERENCES product_variants(id),
    CONSTRAINT chk_order_items_quantity
        CHECK (quantity > 0),
    CONSTRAINT chk_order_items_unit_price
        CHECK (unit_price >= 0),
    CONSTRAINT chk_order_items_line_total
        CHECK (line_total = unit_price * quantity)
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_variant_id ON order_items(variant_id);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    amount NUMERIC(14, 2) NOT NULL,
    transaction_ref VARCHAR(100),
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payments_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_payments_method
        CHECK (method IN ('COD', 'ONLINE')),
    CONSTRAINT chk_payments_status
        CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED')),
    CONSTRAINT chk_payments_amount
        CHECK (amount >= 0)
);

CREATE INDEX idx_payments_status ON payments(status);

CREATE TABLE voucher_usages (
    id BIGSERIAL PRIMARY KEY,
    voucher_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL UNIQUE,
    discount_amount NUMERIC(14, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_voucher_usages_voucher
        FOREIGN KEY (voucher_id) REFERENCES vouchers(id),
    CONSTRAINT fk_voucher_usages_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_voucher_usages_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    -- Mỗi khách chỉ dùng mỗi mã một lần (bản ghi bị xóa nếu đơn bị hủy).
    CONSTRAINT uq_voucher_usages_voucher_user
        UNIQUE (voucher_id, user_id),
    CONSTRAINT chk_voucher_usages_discount_amount
        CHECK (discount_amount >= 0)
);

CREATE INDEX idx_voucher_usages_user_id ON voucher_usages(user_id);
