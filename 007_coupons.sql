-- ============================================================================
-- Script: 007_coupons.sql
-- Domain: Promotion, Coupons & Loyalty Gift Points Domain
-- Target: PostgreSQL 15+
-- ============================================================================

-- Ensure UUID generator extension is available
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ----------------------------------------------------------------------------
-- 1. Table: coupons
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS coupons (
    coupon_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(50) NOT NULL,
    discount_type       VARCHAR(20) NOT NULL, -- PERCENTAGE, FIXED_AMOUNT
    discount_value      NUMERIC(12, 2) NOT NULL,
    min_order_amount    NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    max_discount_amount NUMERIC(12, 2),
    valid_from          TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until         TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Audit Columns
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    
    -- Optimistic Locking & Soft Delete
    version             BIGINT NOT NULL DEFAULT 0,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP WITH TIME ZONE,

    -- Check Constraints
    CONSTRAINT chk_coupons_discount_val CHECK (discount_value > 0),
    CONSTRAINT chk_coupons_min_order CHECK (min_order_amount >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_coupons_code_active 
    ON coupons (code) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_coupons_code_active 
    ON coupons (code, is_active) 
    WHERE is_deleted = FALSE;


-- ----------------------------------------------------------------------------
-- 2. Table: coupon_redemptions
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS coupon_redemptions (
    redemption_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coupon_id           UUID NOT NULL,
    user_id             UUID NOT NULL,
    order_id            UUID NOT NULL,
    discount_applied    NUMERIC(12, 2) NOT NULL,
    redeemed_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Key Constraints
    CONSTRAINT fk_coupon_redemptions_coupon
        FOREIGN KEY (coupon_id) 
        REFERENCES coupons (coupon_id) 
        ON DELETE RESTRICT,
        
    CONSTRAINT fk_coupon_redemptions_user
        FOREIGN KEY (user_id) 
        REFERENCES users (user_id) 
        ON DELETE RESTRICT,
        
    CONSTRAINT fk_coupon_redemptions_order
        FOREIGN KEY (order_id) 
        REFERENCES orders (order_id) 
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_coupon_redemptions_user 
    ON coupon_redemptions (user_id, coupon_id);

CREATE INDEX IF NOT EXISTS idx_coupon_redemptions_order 
    ON coupon_redemptions (order_id);


-- ----------------------------------------------------------------------------
-- 3. Table: loyalty_accounts (Gift Points / Reward Points)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS loyalty_accounts (
    loyalty_account_id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    points_balance      INTEGER NOT NULL DEFAULT 0,
    
    -- Audit Columns
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    
    -- Optimistic Locking & Soft Delete
    version             BIGINT NOT NULL DEFAULT 0,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP WITH TIME ZONE,

    -- Foreign Key Constraints
    CONSTRAINT fk_loyalty_accounts_user
        FOREIGN KEY (user_id) 
        REFERENCES users (user_id) 
        ON DELETE RESTRICT,

    -- Check Constraints
    CONSTRAINT chk_loyalty_points_positive CHECK (points_balance >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_loyalty_accounts_user_active 
    ON loyalty_accounts (user_id) 
    WHERE is_deleted = FALSE;


-- ----------------------------------------------------------------------------
-- 4. Table: loyalty_transactions
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS loyalty_transactions (
    transaction_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loyalty_account_id  UUID NOT NULL,
    order_id            UUID,
    transaction_type    VARCHAR(20) NOT NULL, -- EARNED, REDEEMED, EXPIRED, REFUNDED
    points              INTEGER NOT NULL,
    balance_after       INTEGER NOT NULL,
    reason              VARCHAR(255),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Key Constraints
    CONSTRAINT fk_loyalty_transactions_account
        FOREIGN KEY (loyalty_account_id) 
        REFERENCES loyalty_accounts (loyalty_account_id) 
        ON DELETE CASCADE,
        
    CONSTRAINT fk_loyalty_transactions_order
        FOREIGN KEY (order_id) 
        REFERENCES orders (order_id) 
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_loyalty_tx_account 
    ON loyalty_transactions (loyalty_account_id, created_at DESC);
