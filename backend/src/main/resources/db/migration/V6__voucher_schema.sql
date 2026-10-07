CREATE TABLE vouchers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    discount_type VARCHAR(20) NOT NULL,
    discount_value NUMERIC(14, 2) NOT NULL,
    min_order_value NUMERIC(14, 2) NOT NULL DEFAULT 0,
    max_discount NUMERIC(14, 2),
    usage_limit INTEGER,
    used_count INTEGER NOT NULL DEFAULT 0,
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_vouchers_discount_type
        CHECK (discount_type IN ('FIXED', 'PERCENTAGE')),
    CONSTRAINT chk_vouchers_discount_value
        CHECK (discount_value > 0),
    CONSTRAINT chk_vouchers_percentage_max_100
        CHECK (discount_type <> 'PERCENTAGE' OR discount_value <= 100),
    CONSTRAINT chk_vouchers_min_order_value
        CHECK (min_order_value >= 0),
    CONSTRAINT chk_vouchers_max_discount
        CHECK (max_discount IS NULL OR max_discount > 0),
    CONSTRAINT chk_vouchers_usage_limit
        CHECK (usage_limit IS NULL OR usage_limit > 0),
    -- V08: DB là chốt chặn cuối cùng khi nhiều đơn dùng voucher cùng lúc.
    CONSTRAINT chk_vouchers_used_count
        CHECK (used_count >= 0 AND (usage_limit IS NULL OR used_count <= usage_limit)),
    -- V05
    CONSTRAINT chk_vouchers_dates
        CHECK (start_date < end_date),
    CONSTRAINT chk_vouchers_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_vouchers_status ON vouchers(status);
