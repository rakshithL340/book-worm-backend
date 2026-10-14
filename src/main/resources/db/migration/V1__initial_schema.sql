-- ============================================================================
-- Flyway Migration: V1__initial_schema.sql
-- Description: Complete Relational Schema (3NF) for Book Worm Platform
-- Database: PostgreSQL 15+
-- ============================================================================

-- Ensure UUID extension is active
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- 1. USERS & IDENTITY
-- ============================================================================
CREATE TABLE IF NOT EXISTS users (
    user_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    role            VARCHAR(30) NOT NULL DEFAULT 'ROLE_CUSTOMER',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_active 
    ON users (email) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_users_role 
    ON users (role) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_users_created_at 
    ON users (created_at DESC);

-- ============================================================================
-- 2. ADDRESSES
-- ============================================================================
CREATE TABLE IF NOT EXISTS addresses (
    address_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    address_line1   VARCHAR(255) NOT NULL,
    address_line2   VARCHAR(255),
    city            VARCHAR(100) NOT NULL,
    state           VARCHAR(100) NOT NULL,
    country         VARCHAR(100) NOT NULL DEFAULT 'India',
    pin_code        VARCHAR(10) NOT NULL,
    phone_number    VARCHAR(20) NOT NULL,
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_addresses_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_addresses_user_id 
    ON addresses (user_id) 
    WHERE is_deleted = FALSE;

-- ============================================================================
-- 3. WISHLISTS
-- ============================================================================
CREATE TABLE IF NOT EXISTS wishlists (
    wishlist_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_wishlists_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_wishlists_user_active 
    ON wishlists (user_id) 
    WHERE is_deleted = FALSE;

-- ============================================================================
-- 4. STORES & STORE POLICIES
-- ============================================================================
CREATE TABLE IF NOT EXISTS stores (
    store_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_code      VARCHAR(50) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    currency_code   VARCHAR(3) NOT NULL DEFAULT 'INR',
    locale          VARCHAR(10) NOT NULL DEFAULT 'en_IN',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_stores_code_active 
    ON stores (store_code) 
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS store_policies (
    policy_id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id                    UUID NOT NULL,
    cancellation_window_hours   INTEGER NOT NULL DEFAULT 48,
    tax_rate_percent            NUMERIC(5, 2) NOT NULL DEFAULT 12.00,
    free_delivery_threshold     NUMERIC(12, 2) NOT NULL DEFAULT 500.00,
    standard_delivery_fee       NUMERIC(12, 2) NOT NULL DEFAULT 50.00,
    loyalty_redemption_rate     NUMERIC(5, 2) NOT NULL DEFAULT 1.00,
    return_window_days          INTEGER NOT NULL DEFAULT 7,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                  VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version                     BIGINT NOT NULL DEFAULT 0,
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at                  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_store_policies_store
        FOREIGN KEY (store_id) REFERENCES stores (store_id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_store_policies_store_active 
    ON store_policies (store_id) 
    WHERE is_deleted = FALSE;

-- Seed Default Store and Policies
INSERT INTO stores (store_id, store_code, name, currency_code, locale, is_active)
VALUES ('10000000-0000-0000-0000-000000000001', 'BW-IN-01', 'Book Worm India Main Store', 'INR', 'en_IN', TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO store_policies (policy_id, store_id, cancellation_window_hours, tax_rate_percent, free_delivery_threshold, standard_delivery_fee, loyalty_redemption_rate, return_window_days)
VALUES ('10000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 48, 12.00, 500.00, 50.00, 1.00, 7)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 5. CATEGORIES
-- ============================================================================
CREATE TABLE IF NOT EXISTS categories (
    category_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(100) NOT NULL,
    display_order   INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
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

-- ============================================================================
-- 6. AUTHORS
-- ============================================================================
CREATE TABLE IF NOT EXISTS authors (
    author_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(150) NOT NULL,
    bio             TEXT,
    avatar_url      VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_authors_name 
    ON authors (name) 
    WHERE is_deleted = FALSE;

-- ============================================================================
-- 7. PUBLISHERS
-- ============================================================================
CREATE TABLE IF NOT EXISTS publishers (
    publisher_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(150) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_publishers_name_active 
    ON publishers (name) 
    WHERE is_deleted = FALSE;

-- ============================================================================
-- 8. BOOKS
-- ============================================================================
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
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_books_author
        FOREIGN KEY (author_id) REFERENCES authors (author_id) ON DELETE RESTRICT,
    CONSTRAINT fk_books_publisher
        FOREIGN KEY (publisher_id) REFERENCES publishers (publisher_id) ON DELETE RESTRICT,
    CONSTRAINT fk_books_category
        FOREIGN KEY (category_id) REFERENCES categories (category_id) ON DELETE RESTRICT,
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

CREATE INDEX IF NOT EXISTS idx_books_sales_count 
    ON books (sales_count DESC) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_books_created_at 
    ON books (created_at DESC) 
    WHERE is_deleted = FALSE;

-- ============================================================================
-- 9. WISHLIST ITEMS
-- ============================================================================
CREATE TABLE IF NOT EXISTS wishlist_items (
    wishlist_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wishlist_id      UUID NOT NULL,
    book_id          UUID NOT NULL,
    added_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by       VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version          BIGINT NOT NULL DEFAULT 0,
    is_deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_wishlist_items_wishlist
        FOREIGN KEY (wishlist_id) REFERENCES wishlists (wishlist_id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_items_book
        FOREIGN KEY (book_id) REFERENCES books (book_id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_wishlist_items_book_active 
    ON wishlist_items (wishlist_id, book_id) 
    WHERE is_deleted = FALSE;

-- ============================================================================
-- 10. CARTS & CART ITEMS
-- ============================================================================
CREATE TABLE IF NOT EXISTS carts (
    cart_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID,
    session_token   VARCHAR(100),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_carts_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_carts_user_active 
    ON carts (user_id) 
    WHERE is_active = TRUE AND is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_carts_session_token 
    ON carts (session_token) 
    WHERE is_active = TRUE AND is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS cart_items (
    cart_item_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id         UUID NOT NULL,
    book_id         UUID NOT NULL,
    quantity        INTEGER NOT NULL DEFAULT 1,
    unit_price      NUMERIC(12, 2) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (cart_id) REFERENCES carts (cart_id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_book
        FOREIGN KEY (book_id) REFERENCES books (book_id) ON DELETE RESTRICT,
    CONSTRAINT chk_cart_items_quantity CHECK (quantity >= 1)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_cart_items_cart_book_active 
    ON cart_items (cart_id, book_id) 
    WHERE is_deleted = FALSE;

-- ============================================================================
-- 11. ORDERS & ORDER ITEMS
-- ============================================================================
CREATE TABLE IF NOT EXISTS orders (
    order_id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number            VARCHAR(50) NOT NULL,
    user_id                 UUID NOT NULL,
    store_id                UUID NOT NULL,
    shipping_address_id     UUID NOT NULL,
    status                  VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    subtotal                NUMERIC(12, 2) NOT NULL,
    tax_amount              NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    delivery_charge         NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    discount_amount         NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total_amount            NUMERIC(12, 2) NOT NULL,
    tentative_delivery_date DATE NOT NULL,
    cancelled_at            TIMESTAMP WITH TIME ZONE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version                 BIGINT NOT NULL DEFAULT 0,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_store
        FOREIGN KEY (store_id) REFERENCES stores (store_id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_address
        FOREIGN KEY (shipping_address_id) REFERENCES addresses (address_id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_orders_number_active 
    ON orders (order_number) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_orders_user_created 
    ON orders (user_id, created_at DESC) 
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS order_items (
    order_item_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL,
    book_id         UUID NOT NULL,
    quantity        INTEGER NOT NULL,
    unit_price      NUMERIC(12, 2) NOT NULL,
    total_price     NUMERIC(12, 2) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_book
        FOREIGN KEY (book_id) REFERENCES books (book_id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS order_status_history (
    history_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL,
    status          VARCHAR(30) NOT NULL,
    notes           TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_order_history_order
        FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE CASCADE
);

-- ============================================================================
-- 12. WALLETS & PAYMENTS
-- ============================================================================
CREATE TABLE IF NOT EXISTS wallet_accounts (
    wallet_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    balance         NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    currency_code   VARCHAR(3) NOT NULL DEFAULT 'INR',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_wallets_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT chk_wallet_balance CHECK (balance >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_wallets_user_active 
    ON wallet_accounts (user_id) 
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS wallet_transactions (
    transaction_id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id       UUID NOT NULL,
    order_id        UUID,
    transaction_type VARCHAR(20) NOT NULL, -- CREDIT, DEBIT, REFUND
    amount          NUMERIC(12, 2) NOT NULL,
    balance_after   NUMERIC(12, 2) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_tx_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallet_accounts (wallet_id) ON DELETE RESTRICT,
    CONSTRAINT fk_wallet_tx_order
        FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS payment_transactions (
    transaction_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id            UUID NOT NULL,
    payment_method      VARCHAR(30) NOT NULL, -- CREDIT_CARD, DEBIT_CARD, UPI, WALLET
    amount              NUMERIC(12, 2) NOT NULL,
    currency_code       VARCHAR(3) NOT NULL DEFAULT 'INR',
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    gateway_ref_id      VARCHAR(100),
    masked_card_number  VARCHAR(25),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version             BIGINT NOT NULL DEFAULT 0,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_payments_order
        FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS refund_transactions (
    refund_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id          UUID NOT NULL,
    refund_amount       NUMERIC(12, 2) NOT NULL,
    refund_reference    VARCHAR(100) NOT NULL,
    reason              VARCHAR(255),
    status              VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refunds_payment
        FOREIGN KEY (payment_id) REFERENCES payment_transactions (transaction_id) ON DELETE RESTRICT
);

-- ============================================================================
-- 13. SHIPPING & LOGISTICS
-- ============================================================================
CREATE TABLE IF NOT EXISTS shipments (
    shipment_id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id                UUID NOT NULL,
    carrier_name            VARCHAR(100) NOT NULL,
    tracking_number         VARCHAR(100) NOT NULL,
    status                  VARCHAR(30) NOT NULL DEFAULT 'LABEL_CREATED',
    estimated_delivery_date DATE NOT NULL,
    dispatched_at           TIMESTAMP WITH TIME ZONE,
    delivered_at            TIMESTAMP WITH TIME ZONE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version                 BIGINT NOT NULL DEFAULT 0,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_shipments_order
        FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_shipments_tracking_active 
    ON shipments (tracking_number) 
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS tracking_events (
    event_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id     UUID NOT NULL,
    event_status    VARCHAR(50) NOT NULL,
    location        VARCHAR(150),
    event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    description     TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tracking_events_shipment
        FOREIGN KEY (shipment_id) REFERENCES shipments (shipment_id) ON DELETE CASCADE
);

-- ============================================================================
-- 14. REVIEWS (100-Character Constraint)
-- ============================================================================
CREATE TABLE IF NOT EXISTS reviews (
    review_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id         UUID NOT NULL,
    user_id         UUID NOT NULL,
    reviewer_name   VARCHAR(100) NOT NULL,
    rating          INTEGER NOT NULL,
    review_text     VARCHAR(100) NOT NULL, -- Strict 100 character maximum
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_reviews_book
        FOREIGN KEY (book_id) REFERENCES books (book_id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT chk_reviews_rating CHECK (rating >= 1 AND rating <= 5)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_reviews_book_user_active 
    ON reviews (book_id, user_id) 
    WHERE is_deleted = FALSE;

-- ============================================================================
-- 15. COUPONS & LOYALTY POINTS
-- ============================================================================
CREATE TABLE IF NOT EXISTS coupons (
    coupon_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(50) NOT NULL,
    discount_type       VARCHAR(20) NOT NULL, -- PERCENTAGE, FLAT
    discount_value      NUMERIC(12, 2) NOT NULL,
    min_order_amount    NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    max_discount_amount NUMERIC(12, 2),
    valid_from          TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until         TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version             BIGINT NOT NULL DEFAULT 0,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_coupons_code_active 
    ON coupons (code) 
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS coupon_redemptions (
    redemption_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coupon_id           UUID NOT NULL,
    user_id             UUID NOT NULL,
    order_id            UUID NOT NULL,
    discount_applied    NUMERIC(12, 2) NOT NULL,
    redeemed_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_redemptions_coupon
        FOREIGN KEY (coupon_id) REFERENCES coupons (coupon_id) ON DELETE RESTRICT,
    CONSTRAINT fk_redemptions_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_redemptions_order
        FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS loyalty_accounts (
    loyalty_account_id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    points_balance      INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version             BIGINT NOT NULL DEFAULT 0,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_loyalty_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT chk_loyalty_points CHECK (points_balance >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_loyalty_user_active 
    ON loyalty_accounts (user_id) 
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS loyalty_transactions (
    transaction_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loyalty_account_id  UUID NOT NULL,
    order_id            UUID,
    transaction_type    VARCHAR(20) NOT NULL, -- EARNED, REDEEMED, REFUNDED
    points              INTEGER NOT NULL,
    balance_after       INTEGER NOT NULL,
    reason              VARCHAR(255),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loyalty_tx_account
        FOREIGN KEY (loyalty_account_id) REFERENCES loyalty_accounts (loyalty_account_id) ON DELETE RESTRICT,
    CONSTRAINT fk_loyalty_tx_order
        FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE SET NULL
);
