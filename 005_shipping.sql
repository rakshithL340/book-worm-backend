-- ============================================================================
-- Script: 005_shipping.sql
-- Domain: Shipping & Logistics Fulfillment Domain
-- Target: PostgreSQL 15+
-- ============================================================================

-- Ensure UUID generator extension is available
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ----------------------------------------------------------------------------
-- 1. Table: shipments
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS shipments (
    shipment_id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id                UUID NOT NULL,
    carrier_name            VARCHAR(100) NOT NULL,
    tracking_number         VARCHAR(100) NOT NULL,
    status                  VARCHAR(30) NOT NULL DEFAULT 'LABEL_CREATED', -- LABEL_CREATED, DISPATCHED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, RETURN_IN_TRANSIT, RETURN_RECEIVED
    estimated_delivery_date DATE NOT NULL,
    dispatched_at           TIMESTAMP WITH TIME ZONE,
    delivered_at            TIMESTAMP WITH TIME ZONE,
    
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
    CONSTRAINT fk_shipments_order
        FOREIGN KEY (order_id) 
        REFERENCES orders (order_id) 
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_shipments_order_active 
    ON shipments (order_id) 
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_shipments_tracking_active 
    ON shipments (tracking_number) 
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_shipments_status 
    ON shipments (status) 
    WHERE is_deleted = FALSE;


-- ----------------------------------------------------------------------------
-- 2. Table: tracking_events
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tracking_events (
    event_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id     UUID NOT NULL,
    event_status    VARCHAR(50) NOT NULL,
    location        VARCHAR(150),
    event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    description     TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Key Constraints
    CONSTRAINT fk_tracking_events_shipment
        FOREIGN KEY (shipment_id) 
        REFERENCES shipments (shipment_id) 
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tracking_events_shipment 
    ON tracking_events (shipment_id, event_timestamp DESC);
