-- ============================================================================
-- Script: 001_users.sql
-- Domain: Member & Identity Domain (Users, Addresses, Wishlists)
-- Target: PostgreSQL 15+
-- ============================================================================

-- Ensure UUID generator extension is available
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ----------------------------------------------------------------------------
-- 1. Table: users
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    role            VARCHAR(30) NOT NULL DEFAULT 'ROLE_MEMBER',
    
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

-- Partial Unique Index on email for active (non-deleted) records
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_active 
    ON users (email) 
    WHERE is_deleted = FALSE;

-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_users_role 
    ON users (role) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_users_created_at 
    ON users (created_at DESC);


-- ----------------------------------------------------------------------------
-- 2. Table: addresses
-- ----------------------------------------------------------------------------
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
    CONSTRAINT fk_addresses_user
        FOREIGN KEY (user_id) 
        REFERENCES users (user_id) 
        ON DELETE RESTRICT
);

-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_addresses_user_id 
    ON addresses (user_id) 
    WHERE is_deleted = FALSE;


-- ----------------------------------------------------------------------------
-- 3. Table: wishlists
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wishlists (
    wishlist_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    
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
    CONSTRAINT fk_wishlists_user
        FOREIGN KEY (user_id) 
        REFERENCES users (user_id) 
        ON DELETE RESTRICT
);

-- Unique Wishlist per User (Active)
CREATE UNIQUE INDEX IF NOT EXISTS uq_wishlists_user_active 
    ON wishlists (user_id) 
    WHERE is_deleted = FALSE;
