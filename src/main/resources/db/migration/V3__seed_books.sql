-- ============================================================================
-- Flyway Migration: V3__seed_books.sql
-- Description: Seed Core Book Catalog Data with Pricing, Formats, and Stocks
-- Database: PostgreSQL 15+
-- ============================================================================

-- Ensure Referenced Authors Exist
INSERT INTO authors (author_id, name, bio) VALUES
('a1000000-0000-0000-0000-000000000001', 'J.K. Rowling', 'British author best known for the Harry Potter series.'),
('a1000000-0000-0000-0000-000000000002', 'George R.R. Martin', 'American novelist and screenwriter of A Song of Ice and Fire.'),
('a1000000-0000-0000-0000-000000000003', 'James Clear', 'Author and speaker focused on habits and decision making.'),
('a1000000-0000-0000-0000-000000000004', 'Yuval Noah Harari', 'Historian, philosopher, and bestselling author of Sapiens.'),
('a1000000-0000-0000-0000-000000000005', 'Morgan Housel', 'Partner at Collaborative Fund and financial writer.'),
('a1000000-0000-0000-0000-000000000006', 'Amish Tripathi', 'Indian author known for the Shiva Trilogy and Ram Chandra Series.'),
('a1000000-0000-0000-0000-000000000007', 'Robert C. Martin', 'Software engineer and author known as Uncle Bob.'),
('a1000000-0000-0000-0000-000000000008', 'Agatha Christie', 'English writer known for sixty-six detective novels.'),
('a1000000-0000-0000-0000-000000000009', 'Paulo Coelho', 'Brazilian lyricist and novelist, author of The Alchemist.'),
('a1000000-0000-0000-0000-000000000010', 'Cal Newport', 'Computer science professor and author of Deep Work.')
ON CONFLICT (author_id) DO NOTHING;

-- Ensure Referenced Publishers Exist
INSERT INTO publishers (publisher_id, name) VALUES
('p1000000-0000-0000-0000-000000000001', 'Penguin Random House'),
('p1000000-0000-0000-0000-000000000002', 'HarperCollins'),
('p1000000-0000-0000-0000-000000000003', 'Simon & Schuster'),
('p1000000-0000-0000-0000-000000000004', 'Macmillan Publishers'),
('p1000000-0000-0000-0000-000000000005', 'Hachette Book Group'),
('p1000000-0000-0000-0000-000000000006', 'Oxford University Press'),
('p1000000-0000-0000-0000-000000000007', 'Rupa Publications'),
('p1000000-0000-0000-0000-000000000008', 'Jaico Publishing House')
ON CONFLICT (publisher_id) DO NOTHING;

-- Seed Catalog Books
INSERT INTO books (
    book_id, title, isbn, author_id, publisher_id, category_id,
    format, price, language, cover_image_url, description,
    sales_count, average_rating, rating_count, stock_quantity
) VALUES
(
    'b1000000-0000-0000-0000-000000000001',
    'Atomic Habits: An Easy & Proven Way to Build Good Habits & Break Bad Ones',
    '9780735211292',
    'a1000000-0000-0000-0000-000000000003',
    'p1000000-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000007',
    'HARDCOVER',
    599.00,
    'English',
    'https://images.example.com/books/atomic-habits.jpg',
    'No matter your goals, Atomic Habits offers a proven framework for improving every day.',
    1250,
    4.85,
    340,
    150
),
(
    'b1000000-0000-0000-0000-000000000002',
    'Sapiens: A Brief History of Humankind',
    '9780062316097',
    'a1000000-0000-0000-0000-000000000004',
    'p1000000-0000-0000-0000-000000000002',
    'c1000000-0000-0000-0000-000000000009',
    'PAPERBACK',
    499.00,
    'English',
    'https://images.example.com/books/sapiens.jpg',
    'From a renowned historian comes a groundbreaking narrative of humanity''s creation and evolution.',
    980,
    4.70,
    215,
    80
),
(
    'b1000000-0000-0000-0000-000000000003',
    'The Psychology of Money: Timeless Lessons on Wealth, Greed, and Happiness',
    '9789390166268',
    'a1000000-0000-0000-0000-000000000005',
    'p1000000-0000-0000-0000-000000000008',
    'c1000000-0000-0000-0000-000000000008',
    'PAPERBACK',
    350.00,
    'English',
    'https://images.example.com/books/psychology-of-money.jpg',
    'Doing well with money isn''t necessarily about what you know. It''s about how you behave.',
    1100,
    4.80,
    290,
    120
),
(
    'b1000000-0000-0000-0000-000000000004',
    'Clean Code: A Handbook of Agile Software Craftsmanship',
    '9780132350884',
    'a1000000-0000-0000-0000-000000000007',
    'p1000000-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000010',
    'PAPERBACK',
    899.00,
    'English',
    'https://images.example.com/books/clean-code.jpg',
    'Even bad code can function. But if code isn''t clean, it can bring a development organization to its knees.',
    650,
    4.65,
    140,
    45
),
(
    'b1000000-0000-0000-0000-000000000005',
    'The Immortals of Meluha (Shiva Trilogy Book 1)',
    '9789380658742',
    'a1000000-0000-0000-0000-000000000006',
    'p1000000-0000-0000-0000-000000000007',
    'c1000000-0000-0000-0000-000000000001',
    'PAPERBACK',
    399.00,
    'English',
    'https://images.example.com/books/immortals-of-meluha.jpg',
    '1900 BC. In what modern Indians mistakenly call the Indus Valley Civilisation.',
    1450,
    4.60,
    420,
    200
),
(
    'b1000000-0000-0000-0000-000000000006',
    'Deep Work: Rules for Focused Success in a Distracted World',
    '9781455586691',
    'a1000000-0000-0000-0000-000000000010',
    'p1000000-0000-0000-0000-000000000005',
    'c1000000-0000-0000-0000-000000000007',
    'HARDCOVER',
    550.00,
    'English',
    'https://images.example.com/books/deep-work.jpg',
    'Deep work is the ability to focus without distraction on a cognitively demanding task.',
    780,
    4.72,
    180,
    60
),
(
    'b1000000-0000-0000-0000-000000000007',
    'The Alchemist',
    '9780062315007',
    'a1000000-0000-0000-0000-000000000009',
    'p1000000-0000-0000-0000-000000000002',
    'c1000000-0000-0000-0000-000000000001',
    'PAPERBACK',
    299.00,
    'English',
    'https://images.example.com/books/the-alchemist.jpg',
    'A magical story about following your dream and listening to your heart.',
    2100,
    4.75,
    650,
    300
),
(
    'b1000000-0000-0000-0000-000000000008',
    'And Then There Were None',
    '9780062073488',
    'a1000000-0000-0000-0000-000000000008',
    'p1000000-0000-0000-0000-000000000002',
    'c1000000-0000-0000-0000-000000000003',
    'PAPERBACK',
    349.00,
    'English',
    'https://images.example.com/books/and-then-there-were-none.jpg',
    'Ten strangers are invited to an isolated island off the Devon coast by a mysterious host.',
    890,
    4.82,
    220,
    75
),
(
    'b1000000-0000-0000-0000-000000000009',
    'A Game of Thrones: A Song of Ice and Fire',
    '9780553103540',
    'a1000000-0000-0000-0000-000000000002',
    'p1000000-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000004',
    'HARDCOVER',
    799.00,
    'English',
    'https://images.example.com/books/game-of-thrones.jpg',
    'Winter is coming. Such is the stern motto of House Stark, the northernmost fiefdom of the realm.',
    1320,
    4.88,
    410,
    90
),
(
    'b1000000-0000-0000-0000-000000000010',
    'Harry Potter and the Philosopher''s Stone',
    '9780747532743',
    'a1000000-0000-0000-0000-000000000001',
    'p1000000-0000-0000-0000-000000000005',
    'c1000000-0000-0000-0000-000000000015',
    'HARDCOVER',
    650.00,
    'English',
    'https://images.example.com/books/harry-potter-1.jpg',
    'Harry Potter has never even heard of Hogwarts when letters start dropping on the doormat at number four, Privet Drive.',
    2800,
    4.92,
    890,
    250
)
ON CONFLICT (book_id) DO UPDATE SET
    title = EXCLUDED.title,
    price = EXCLUDED.price,
    sales_count = EXCLUDED.sales_count,
    average_rating = EXCLUDED.average_rating,
    rating_count = EXCLUDED.rating_count,
    stock_quantity = EXCLUDED.stock_quantity;
