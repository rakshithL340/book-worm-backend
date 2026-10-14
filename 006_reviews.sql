-- ============================================================================
-- Script: 006_reviews.sql
-- Domain: Social Proof, Customer Reviews & Ratings Domain
-- Target: PostgreSQL 15+
-- ============================================================================

-- Ensure UUID generator extension is available
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ----------------------------------------------------------------------------
-- 1. Table: reviews (Strict 100-character constraint & 1-5 star ratings)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reviews (
    review_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id         UUID NOT NULL,
    user_id         UUID NOT NULL,
    reviewer_name   VARCHAR(100) NOT NULL,
    rating          INTEGER NOT NULL,
    review_text     VARCHAR(100) NOT NULL,
    
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
    CONSTRAINT fk_reviews_book
        FOREIGN KEY (book_id) 
        REFERENCES books (book_id) 
        ON DELETE CASCADE,
        
    CONSTRAINT fk_reviews_user
        FOREIGN KEY (user_id) 
        REFERENCES users (user_id) 
        ON DELETE RESTRICT,

    -- Domain Check Constraints
    CONSTRAINT chk_reviews_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT chk_reviews_text_length CHECK (char_length(review_text) <= 100)
);

CREATE INDEX IF NOT EXISTS idx_reviews_book_created 
    ON reviews (book_id, created_at DESC) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_reviews_user 
    ON reviews (user_id) 
    WHERE is_deleted = FALSE;
