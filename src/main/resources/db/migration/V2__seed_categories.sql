-- ============================================================================
-- Flyway Migration: V2__seed_categories.sql
-- Description: Seed Fixed 20-Category Taxonomy for Navigation and Filtering
-- Database: PostgreSQL 15+
-- ============================================================================

INSERT INTO categories (category_id, name, slug, display_order, is_active) VALUES
('c1000000-0000-0000-0000-000000000001', 'Fiction', 'fiction', 1, TRUE),
('c1000000-0000-0000-0000-000000000002', 'Non-Fiction', 'non-fiction', 2, TRUE),
('c1000000-0000-0000-0000-000000000003', 'Mystery & Thriller', 'mystery-thriller', 3, TRUE),
('c1000000-0000-0000-0000-000000000004', 'Sci-Fi & Fantasy', 'sci-fi-fantasy', 4, TRUE),
('c1000000-0000-0000-0000-000000000005', 'Romance', 'romance', 5, TRUE),
('c1000000-0000-0000-0000-000000000006', 'Biography & Memoir', 'biography-memoir', 6, TRUE),
('c1000000-0000-0000-0000-000000000007', 'Self-Help & Personal Development', 'self-help', 7, TRUE),
('c1000000-0000-0000-0000-000000000008', 'Business & Economics', 'business-economics', 8, TRUE),
('c1000000-0000-0000-0000-000000000009', 'History', 'history', 9, TRUE),
('c1000000-0000-0000-0000-000000000010', 'Science & Technology', 'science-technology', 10, TRUE),
('c1000000-0000-0000-0000-000000000011', 'Philosophy', 'philosophy', 11, TRUE),
('c1000000-0000-0000-0000-000000000012', 'Psychology', 'psychology', 12, TRUE),
('c1000000-0000-0000-0000-000000000013', 'Health & Fitness', 'health-fitness', 13, TRUE),
('c1000000-0000-0000-0000-000000000014', 'Cookbooks & Culinary', 'cookbooks-culinary', 14, TRUE),
('c1000000-0000-0000-0000-000000000015', 'Children''s Books', 'childrens-books', 15, TRUE),
('c1000000-0000-0000-0000-000000000016', 'Young Adult (YA)', 'young-adult', 16, TRUE),
('c1000000-0000-0000-0000-000000000017', 'Graphic Novels & Comics', 'graphic-novels-comics', 17, TRUE),
('c1000000-0000-0000-0000-000000000018', 'Poetry', 'poetry', 18, TRUE),
('c1000000-0000-0000-0000-000000000019', 'Travel & Adventure', 'travel-adventure', 19, TRUE),
('c1000000-0000-0000-0000-000000000020', 'Religion & Spirituality', 'religion-spirituality', 20, TRUE)
ON CONFLICT (category_id) DO UPDATE SET
    name = EXCLUDED.name,
    slug = EXCLUDED.slug,
    display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active;
