-- ============================================================================
-- Flyway Migration: V4__seed_authors.sql
-- Description: Seed Author Directory & Biography Details ("About the writer")
-- Database: PostgreSQL 15+
-- ============================================================================

INSERT INTO authors (author_id, name, bio, avatar_url) VALUES
(
    'a1000000-0000-0000-0000-000000000001',
    'J.K. Rowling',
    'J.K. Rowling is the author of the enduringly popular Harry Potter books, which have sold over 500 million copies and been translated into over 80 languages.',
    'https://images.example.com/authors/jk-rowling.jpg'
),
(
    'a1000000-0000-0000-0000-000000000002',
    'George R.R. Martin',
    'George R.R. Martin is the #1 New York Times bestselling author of many novels, including the acclaimed series A Song of Ice and Fire.',
    'https://images.example.com/authors/grrm.jpg'
),
(
    'a1000000-0000-0000-0000-000000000003',
    'James Clear',
    'James Clear is a writer and speaker focused on habits, decision making, and continuous improvement. He is the author of the #1 New York Times bestseller Atomic Habits.',
    'https://images.example.com/authors/james-clear.jpg'
),
(
    'a1000000-0000-0000-0000-000000000004',
    'Yuval Noah Harari',
    'Prof. Yuval Noah Harari is a historian, philosopher, and the bestselling author of Sapiens: A Brief History of Humankind, Homo Deus, and 21 Lessons for the 21st Century.',
    'https://images.example.com/authors/yuval-harari.jpg'
),
(
    'a1000000-0000-0000-0000-000000000005',
    'Morgan Housel',
    'Morgan Housel is a partner at Collaborative Fund. He is a two-time winner of the Best in Business Award from the Society of American Business Editors and Writers.',
    'https://images.example.com/authors/morgan-housel.jpg'
),
(
    'a1000000-0000-0000-0000-000000000006',
    'Amish Tripathi',
    'Amish Tripathi is an Indian author who wrote the fastest-selling book series in Indian publishing history: The Shiva Trilogy and Ram Chandra Series.',
    'https://images.example.com/authors/amish-tripathi.jpg'
),
(
    'a1000000-0000-0000-0000-000000000007',
    'Robert C. Martin',
    'Robert C. Martin (Uncle Bob) has been a programmer since 1970. He is a co-founder of the Agile Manifesto and author of Clean Code and Clean Architecture.',
    'https://images.example.com/authors/uncle-bob.jpg'
),
(
    'a1000000-0000-0000-0000-000000000008',
    'Agatha Christie',
    'Dame Agatha Mary Clarissa Christie was an English writer known for her sixty-six detective novels and fourteen short story collections, particularly those revolving around Hercule Poirot and Miss Marple.',
    'https://images.example.com/authors/agatha-christie.jpg'
),
(
    'a1000000-0000-0000-0000-000000000009',
    'Paulo Coelho',
    'Paulo Coelho is a Brazilian lyricist and novelist. He is best known for his novel The Alchemist, which became an international bestseller.',
    'https://images.example.com/authors/paulo-coelho.jpg'
),
(
    'a1000000-0000-0000-0000-000000000010',
    'Cal Newport',
    'Cal Newport is an Associate Professor of Computer Science at Georgetown University and the author of Deep Work, Digital Minimalism, and So Good They Can''t Ignore You.',
    'https://images.example.com/authors/cal-newport.jpg'
)
ON CONFLICT (author_id) DO UPDATE SET
    name = EXCLUDED.name,
    bio = EXCLUDED.bio,
    avatar_url = EXCLUDED.avatar_url;
