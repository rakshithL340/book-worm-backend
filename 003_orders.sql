-- ============================================================================
-- Script: 003_orders.sql
-- Domain: Order Lifecycle & Management Domain
-- Target: PostgreSQL 15+
-- ============================================================================

-- Ensure UUID generator extension is available
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ----------------------------------------------------------------------------
-- 1. Table: orders
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    order_id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number             VARCHAR(50) NOT NULL,
    user_id                  UUID NOT NULL,
    store_id                 UUID NOT NULL,
    shipping_address_id      UUID NOT NULL,
    status                   VARCHAR(30) NOT NULL DEFAULT 'CREATED', -- CREATED, PENDING_PAYMENT, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED, RETURNED
    subtotal                 NUMERIC(12, 2) NOT NULL,
    tax_amount               NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    delivery_charge          NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    discount_amount          NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total_amount             NUMERIC(12, 2) NOT NULL,
    tentative_delivery_date  DATE NOT NULL,
    cancelled_at             TIMESTAMP WITH TIME ZONE,
    
    -- Audit Columns
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by               VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by               VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    
    -- Optimistic Locking & Soft Delete
    version                  BIGINT NOT NULL DEFAULT 0,
    is_deleted               BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at               TIMESTAMP WITH TIME ZONE,

    -- Foreign Key Constraints
    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id) 
        REFERENCES users (user_id) 
        ON DELETE RESTRICT,
        
    CONSTRAINT fk_orders_store
        FOREIGN KEY (store_id) 
        REFERENCES stores (store_id) 
        ON DELETE RESTRICT,
        
    CONSTRAINT fk_orders_address
        FOREIGN KEY (shipping_address_id) 
        REFERENCES addresses (address_id) 
        ON DELETE RESTRICT,

    -- Check Constraints
    CONSTRAINT chk_orders_subtotal CHECK (subtotal >= 0),
    CONSTRAINT chk_orders_total CHECK (total_amount >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_orders_order_number_active 
    ON orders (order_number) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_orders_user_created 
    ON orders (user_id, created_at DESC) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_orders_status 
    ON orders (status) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_orders_created_at 
    ON orders (created_at DESC);


-- ----------------------------------------------------------------------------
-- 2. Table: order_items (Immutable point-in-time purchase snapshots)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_items (
    order_item_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL,
    book_id         UUID NOT NULL,
    book_title      VARCHAR(255) NOT NULL,
    book_format     VARCHAR(30) NOT NULL,
    quantity        INTEGER NOT NULL,
    unit_price      NUMERIC(12, 2) NOT NULL,
    total_price     NUMERIC(12, 2) NOT NULL,
    
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
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) 
        REFERENCES orders (order_id) 
        ON DELETE CASCADE,
        
    CONSTRAINT fk_order_items_book
        FOREIGN KEY (book_id) 
        REFERENCES books (book_id) 
        ON DELETE RESTRICT,

    -- Check Constraints
    CONSTRAINT chk_order_items_qty CHECK (quantity >= 1),
    CONSTRAINT chk_order_items_price CHECK (unit_price >= 0 AND total_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id 
    ON order_items (order_id) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_order_items_book_id 
    ON order_items (book_id);


-- ----------------------------------------------------------------------------
-- 3. Table: order_status_histories (Append-only state transition audit log)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_status_histories (
    history_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL,
    from_status     VARCHAR(30),
    to_status       VARCHAR(30) NOT NULL,
    reason          TEXT,
    changed_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Key Constraints
    CONSTRAINT fk_order_status_history_order
        FOREIGN KEY (order_id) 
        REFERENCES orders (order_id) 
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_status_histories_order 
    ON order_status_histories (order_id, created_at);
