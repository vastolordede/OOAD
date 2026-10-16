CREATE TABLE carts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_carts_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_carts_status
        CHECK (status IN ('ACTIVE', 'CHECKED_OUT'))
);

CREATE INDEX idx_carts_user_id ON carts(user_id);

-- C03: mỗi Customer chỉ có tối đa một Cart ACTIVE.
CREATE UNIQUE INDEX uq_carts_one_active_per_user
    ON carts(user_id)
    WHERE status = 'ACTIVE';

CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_variant
        FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE,
    -- C04: một variant chỉ xuất hiện một lần trong một giỏ.
    CONSTRAINT uq_cart_items_cart_variant
        UNIQUE (cart_id, variant_id),
    CONSTRAINT chk_cart_items_quantity
        CHECK (quantity > 0)
);

CREATE INDEX idx_cart_items_variant_id ON cart_items(variant_id);
