-- ============================================================================
-- Script: 004_payments.sql
-- Domain: Payment Processing & Wallet Domain
-- Target: PostgreSQL 15+
-- ============================================================================

-- Ensure UUID generator extension is available
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ----------------------------------------------------------------------------
-- 1. Table: wallet_accounts
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wallet_accounts (
    wallet_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    balance         NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    currency_code   VARCHAR(3) NOT NULL DEFAULT 'INR',
    
    -- Audit Columns
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    
    -- Optimistic Locking & Soft Delete
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE,

    -- Foreign Key Constraints
    CONSTRAINT fk_wallet_accounts_user
        FOREIGN KEY (user_id) 
        REFERENCES users (user_id) 
        ON DELETE RESTRICT,

    -- Check Constraints
    CONSTRAINT chk_wallet_balance CHECK (balance >= 0.00)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_wallet_accounts_user_active 
    ON wallet_accounts (user_id) 
    WHERE is_deleted = FALSE;


-- ----------------------------------------------------------------------------
-- 2. Table: wallet_transactions
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wallet_transactions (
    transaction_id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id       UUID NOT NULL,
    order_id        UUID,
    transaction_type VARCHAR(20) NOT NULL, -- CREDIT, DEBIT, REFUND
    amount          NUMERIC(12, 2) NOT NULL,
    balance_after   NUMERIC(12, 2) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Key Constraints
    CONSTRAINT fk_wallet_transactions_wallet
        FOREIGN KEY (wallet_id) 
        REFERENCES wallet_accounts (wallet_id) 
        ON DELETE CASCADE,
        
    CONSTRAINT fk_wallet_transactions_order
        FOREIGN KEY (order_id) 
        REFERENCES orders (order_id) 
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_wallet_tx_wallet 
    ON wallet_transactions (wallet_id, created_at DESC);


-- ----------------------------------------------------------------------------
-- 3. Table: payment_transactions (PCI-DSS tokenized - no raw card/CVV storage)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payment_transactions (
    transaction_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id            UUID NOT NULL,
    payment_method      VARCHAR(30) NOT NULL, -- CREDIT_CARD, DEBIT_CARD, UPI, WALLET
    amount              NUMERIC(12, 2) NOT NULL,
    currency_code       VARCHAR(3) NOT NULL DEFAULT 'INR',
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, SUCCESS, FAILED, REFUNDED
    gateway_ref_id      VARCHAR(100),
    masked_card_number  VARCHAR(25), -- e.g. XXXX-XXXX-XXXX-1234
    
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
    CONSTRAINT fk_payment_transactions_order
        FOREIGN KEY (order_id) 
        REFERENCES orders (order_id) 
        ON DELETE RESTRICT,

    -- Check Constraints
    CONSTRAINT chk_payment_amount CHECK (amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_payment_tx_order 
    ON payment_transactions (order_id);

CREATE INDEX IF NOT EXISTS idx_payment_tx_gateway_ref 
    ON payment_transactions (gateway_ref_id);


-- ----------------------------------------------------------------------------
-- 4. Table: refund_transactions
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS refund_transactions (
    refund_id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_transaction_id  UUID NOT NULL,
    order_id                UUID NOT NULL,
    refund_amount           NUMERIC(12, 2) NOT NULL,
    status                  VARCHAR(30) NOT NULL DEFAULT 'INITIATED', -- INITIATED, PROCESSED, FAILED
    reason                  VARCHAR(255),
    
    -- Audit Columns
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    
    -- Optimistic Locking & Soft Delete
    version                 BIGINT NOT NULL DEFAULT 0,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMP WITH TIME ZONE,

    -- Foreign Key Constraints
    CONSTRAINT fk_refund_transactions_payment
        FOREIGN KEY (payment_transaction_id) 
        REFERENCES payment_transactions (transaction_id) 
        ON DELETE RESTRICT,
        
    CONSTRAINT fk_refund_transactions_order
        FOREIGN KEY (order_id) 
        REFERENCES orders (order_id) 
        ON DELETE RESTRICT,

    -- Check Constraints
    CONSTRAINT chk_refund_amount CHECK (refund_amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_refund_tx_order 
    ON refund_transactions (order_id);
