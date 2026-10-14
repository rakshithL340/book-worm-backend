-- ============================================================================
-- Script: 002_catalog.sql
-- Domain: Store Governance, Catalog, Discovery & Cart Domain
-- Target: PostgreSQL 15+
-- ============================================================================

-- Ensure UUID generator extension is available
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ----------------------------------------------------------------------------
-- 1. Table: stores
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stores (
    store_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_code      VARCHAR(50) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    currency_code   VARCHAR(3) NOT NULL DEFAULT 'INR',
    locale          VARCHAR(10) NOT NULL DEFAULT 'en_IN',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Audit Columns
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    
    -- Optimistic Locking & Soft Delete
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_stores_code_active 
    ON stores (store_code) 
    WHERE is_deleted = FALSE;


-- ----------------------------------------------------------------------------
-- 2. Table: store_policies (Configurable & Provisional Parameters)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS store_policies (
    policy_id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id                    UUID NOT NULL,
    cancellation_window_hours   INTEGER NOT NULL DEFAULT 48,
    tax_rate_percent            NUMERIC(5, 2) NOT NULL DEFAULT 12.00,
    free_delivery_threshold     NUMERIC(12, 2) NOT NULL DEFAULT 500.00,
    standard_delivery_fee       NUMERIC(12, 2) NOT NULL DEFAULT 50.00,
    loyalty_redemption_rate     NUMERIC(5, 2) NOT NULL DEFAULT 1.00,
    return_window_days          INTEGER NOT NULL DEFAULT 7,
    
    -- Audit Columns
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                  VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    
    -- Optimistic Locking & Soft Delete
    version                     BIGINT NOT NULL DEFAULT 0,
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at                  TIMESTAMP WITH TIME ZONE,

    -- Foreign Key Constraints
    CONSTRAINT fk_store_policies_store
        FOREIGN KEY (store_id) 
        REFERENCES stores (store_id) 
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_store_policies_store_active 
    ON store_policies (store_id) 
    WHERE is_deleted = FALSE;


-- ----------------------------------------------------------------------------
-- 3. Table: categories (20 Taxonomy Categories)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    category_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(100) NOT NULL,
    display_order   INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Audit Columns
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    
    -- Optimistic Locking & Soft Delete
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_categories_name_active 
    ON categories (name) 
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_categories_slug_active 
    ON categories (slug) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_categories_display 
    ON categories (display_order, is_active) 
    WHERE is_deleted = FALSE;


-- ----------------------------------------------------------------------------
-- 4. Table: authors ("About the writer" metadata)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS authors (
    author_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(150) NOT NULL,
    bio             TEXT,
    avatar_url      VARCHAR(500),
    
    -- Audit Columns
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    
    -- Optimistic Locking & Soft Delete
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_authors_name 
    ON authors (name) 
    WHERE is_deleted = FALSE;


-- ----------------------------------------------------------------------------
-- 5. Table: publishers ("Published by")
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS publishers (
    publisher_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(150) NOT NULL,
    
    -- Audit Columns
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    
    -- Optimistic Locking & Soft Delete
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_publishers_name_active 
    ON publishers (name) 
    WHERE is_deleted = FALSE;


-- ----------------------------------------------------------------------------
-- 6. Table: books
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS books (
    book_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(255) NOT NULL,
    isbn            VARCHAR(20) NOT NULL,
    author_id       UUID NOT NULL,
    publisher_id    UUID NOT NULL,
    category_id     UUID NOT NULL,
    format          VARCHAR(30) NOT NULL, -- PAPERBACK, HARDCOVER, EBOOK
    price           NUMERIC(12, 2) NOT NULL,
    language        VARCHAR(50) NOT NULL DEFAULT 'English',
    cover_image_url VARCHAR(500),
    description     TEXT,
    sales_count     BIGINT NOT NULL DEFAULT 0,
    average_rating  NUMERIC(3, 2) NOT NULL DEFAULT 0.00,
    rating_count    INTEGER NOT NULL DEFAULT 0,
    stock_quantity  INTEGER NOT NULL DEFAULT 0,
    
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
    CONSTRAINT fk_books_author
        FOREIGN KEY (author_id) 
        REFERENCES authors (author_id) 
        ON DELETE RESTRICT,
        
    CONSTRAINT fk_books_publisher
        FOREIGN KEY (publisher_id) 
        REFERENCES publishers (publisher_id) 
        ON DELETE RESTRICT,
        
    CONSTRAINT fk_books_category
        FOREIGN KEY (category_id) 
        REFERENCES categories (category_id) 
        ON DELETE RESTRICT,

    -- Check Constraints
    CONSTRAINT chk_books_price CHECK (price >= 0),
    CONSTRAINT chk_books_stock CHECK (stock_quantity >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_books_isbn_active 
    ON books (isbn) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_books_category 
    ON books (category_id) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_books_author 
    ON books (author_id) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_books_price 
    ON books (price) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_books_language 
    ON books (language) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_books_format 
    ON books (format) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_books_sales_count 
    ON books (sales_count DESC) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_books_created_at 
    ON books (created_at DESC) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_books_title 
    ON books (title) 
    WHERE is_deleted = FALSE;


-- ----------------------------------------------------------------------------
-- 7. Table: wishlist_items (M2M between Wishlists and Books)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wishlist_items (
    wishlist_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wishlist_id      UUID NOT NULL,
    book_id          UUID NOT NULL,
    added_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Audit Columns
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by       VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    
    -- Optimistic Locking & Soft Delete
    version          BIGINT NOT NULL DEFAULT 0,
    is_deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMP WITH TIME ZONE,

    -- Foreign Key Constraints
    CONSTRAINT fk_wishlist_items_wishlist
        FOREIGN KEY (wishlist_id) 
        REFERENCES wishlists (wishlist_id) 
        ON DELETE CASCADE,
        
    CONSTRAINT fk_wishlist_items_book
        FOREIGN KEY (book_id) 
        REFERENCES books (book_id) 
        ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_wishlist_items_book_active 
    ON wishlist_items (wishlist_id, book_id) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_wishlist_items_wishlist 
    ON wishlist_items (wishlist_id) 
    WHERE is_deleted = FALSE;


-- ----------------------------------------------------------------------------
-- 8. Table: carts
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS carts (
    cart_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID,
    session_token   VARCHAR(100),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    
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
    CONSTRAINT fk_carts_user
        FOREIGN KEY (user_id) 
        REFERENCES users (user_id) 
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_carts_user_active 
    ON carts (user_id) 
    WHERE is_active = TRUE AND is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_carts_session_token 
    ON carts (session_token) 
    WHERE is_active = TRUE AND is_deleted = FALSE;


-- ----------------------------------------------------------------------------
-- 9. Table: cart_items (Quantity Stepper `- 1 +`)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cart_items (
    cart_item_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id         UUID NOT NULL,
    book_id         UUID NOT NULL,
    quantity        INTEGER NOT NULL DEFAULT 1,
    unit_price      NUMERIC(12, 2) NOT NULL,
    
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
    CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (cart_id) 
        REFERENCES carts (cart_id) 
        ON DELETE CASCADE,
        
    CONSTRAINT fk_cart_items_book
        FOREIGN KEY (book_id) 
        REFERENCES books (book_id) 
        ON DELETE RESTRICT,

    -- Check Constraints
    CONSTRAINT chk_cart_items_quantity CHECK (quantity >= 1)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_cart_items_cart_book_active 
    ON cart_items (cart_id, book_id) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id 
    ON cart_items (cart_id) 
    WHERE is_deleted = FALSE;
