-- ============================================================================
-- Flyway Migration: V5__seed_publishers.sql
-- Description: Seed Book Publishers Directory ("Published by")
-- Database: PostgreSQL 15+
-- ============================================================================

INSERT INTO publishers (publisher_id, name) VALUES
('p1000000-0000-0000-0000-000000000001', 'Penguin Random House'),
('p1000000-0000-0000-0000-000000000002', 'HarperCollins Publishers'),
('p1000000-0000-0000-0000-000000000003', 'Simon & Schuster'),
('p1000000-0000-0000-0000-000000000004', 'Macmillan Publishers'),
('p1000000-0000-0000-0000-000000000005', 'Hachette Book Group'),
('p1000000-0000-0000-0000-000000000006', 'Oxford University Press'),
('p1000000-0000-0000-0000-000000000007', 'Rupa Publications India'),
('p1000000-0000-0000-0000-000000000008', 'Jaico Publishing House')
ON CONFLICT (publisher_id) DO UPDATE SET
    name = EXCLUDED.name;
