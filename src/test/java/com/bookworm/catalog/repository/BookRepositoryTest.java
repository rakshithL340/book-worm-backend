package com.bookworm.catalog.repository;

import com.bookworm.AbstractIntegrationTest;
import com.bookworm.catalog.entity.Author;
import com.bookworm.catalog.entity.Book;
import com.bookworm.catalog.entity.BookFormat;
import com.bookworm.catalog.entity.Category;
import com.bookworm.catalog.entity.Publisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("BookRepository Integration Tests")
class BookRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    private Book testBook;
    private Category testCategory;
    private Author testAuthor;
    private Publisher testPublisher;

    @BeforeEach
    void setUp() {
        testCategory = categoryRepository.save(Category.builder()
                .name("Integration Testing Category")
                .slug("integration-testing")
                .display_order(99)
                .build());

        testAuthor = authorRepository.save(Author.builder()
                .name("Kent Beck")
                .bio("Pioneer of TDD and Extreme Programming")
                .build());

        testPublisher = publisherRepository.save(Publisher.builder()
                .name("Addison-Wesley")
                .build());

        testBook = bookRepository.save(Book.builder()
                .title("Test Driven Development: By Example")
                .isbn("9780321146533")
                .category(testCategory)
                .author(testAuthor)
                .publisher(testPublisher)
                .price(new BigDecimal("450.00"))
                .format(BookFormat.PAPERBACK)
                .stockQuantity(25)
                .salesCount(350L)
                .averageRating(new BigDecimal("4.80"))
                .ratingCount(95)
                .build());
    }

    @Test
    @DisplayName("Should find book by ISBN")
    void shouldFindBookByIsbn() {
        Optional<Book> found = bookRepository.findByIsbn("9780321146533");

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test Driven Development: By Example");
    }

    @Test
    @DisplayName("Should filter books by category and format")
    void shouldFindWithFilters() {
        Page<Book> books = bookRepository.findWithFilters(
                "integration-testing", null, BookFormat.PAPERBACK,
                new BigDecimal("400.00"), new BigDecimal("500.00"),
                PageRequest.of(0, 10));

        assertThat(books.getContent()).isNotEmpty();
        assertThat(books.getContent().get(0).getTitle()).contains("Test Driven Development");
    }

    @Test
    @DisplayName("Should search books by keyword across title, author, and ISBN")
    void shouldSearchByKeyword() {
        Page<Book> books = bookRepository.searchByKeyword("Kent Beck", PageRequest.of(0, 10));

        assertThat(books.getContent()).isNotEmpty();
        assertThat(books.getContent().get(0).getAuthor().getName()).isEqualTo("Kent Beck");
    }

    @Test
    @DisplayName("Should retrieve top bestsellers ordered by sales count")
    void shouldFindBestsellers() {
        List<Book> bestsellers = bookRepository.findBestsellers(PageRequest.of(0, 5));

        assertThat(bestsellers).isNotEmpty();
    }
}
