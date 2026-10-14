package com.bookworm.catalog.service;

import com.bookworm.catalog.dto.BookCardResponse;
import com.bookworm.catalog.dto.BookDetailResponse;
import com.bookworm.catalog.dto.CategoryResponse;
import com.bookworm.catalog.entity.Author;
import com.bookworm.catalog.entity.Book;
import com.bookworm.catalog.entity.BookFormat;
import com.bookworm.catalog.entity.Category;
import com.bookworm.catalog.entity.Publisher;
import com.bookworm.catalog.mapper.AuthorMapper;
import com.bookworm.catalog.mapper.BookMapper;
import com.bookworm.catalog.mapper.CategoryMapper;
import com.bookworm.catalog.mapper.PublisherMapper;
import com.bookworm.catalog.repository.AuthorRepository;
import com.bookworm.catalog.repository.BookRepository;
import com.bookworm.catalog.repository.CategoryRepository;
import com.bookworm.catalog.repository.PublisherRepository;
import com.bookworm.common.dto.PageResponse;
import com.bookworm.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogService Unit Tests")
class CatalogServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private PublisherRepository publisherRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private AuthorMapper authorMapper;

    @Mock
    private PublisherMapper publisherMapper;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private CatalogServiceImpl catalogService;

    private Category testCategory;
    private Author testAuthor;
    private Publisher testPublisher;
    private Book testBook;
    private BookCardResponse testBookCard;
    private BookDetailResponse testBookDetail;

    @BeforeEach
    void setUp() {
        UUID categoryId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID publisherId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        testCategory = Category.builder()
                .categoryId(categoryId)
                .name("Self-Help")
                .slug("self-help")
                .display_order(1)
                .build();

        testAuthor = Author.builder()
                .authorId(authorId)
                .name("James Clear")
                .bio("Author of Atomic Habits")
                .build();

        testPublisher = Publisher.builder()
                .publisherId(publisherId)
                .name("Penguin Random House")
                .build();

        testBook = Book.builder()
                .bookId(bookId)
                .title("Atomic Habits")
                .isbn("9780735211292")
                .category(testCategory)
                .author(testAuthor)
                .publisher(testPublisher)
                .price(new BigDecimal("599.00"))
                .format(BookFormat.HARDCOVER)
                .stockQuantity(100)
                .salesCount(1500L)
                .averageRating(new BigDecimal("4.85"))
                .ratingCount(340)
                .build();

        testBookCard = BookCardResponse.builder()
                .bookId(bookId)
                .title("Atomic Habits")
                .authorName("James Clear")
                .categoryName("Self-Help")
                .price(new BigDecimal("599.00"))
                .format("HARDCOVER")
                .build();

        testBookDetail = BookDetailResponse.builder()
                .bookId(bookId)
                .title("Atomic Habits")
                .isbn("9780735211292")
                .price(new BigDecimal("599.00"))
                .authorName("James Clear")
                .format("HARDCOVER")
                .build();
    }

    @Test
    @DisplayName("Should return all active categories")
    void shouldReturnAllCategories() {
        // Arrange
        when(categoryRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(testCategory));
        CategoryResponse catDto = CategoryResponse.builder().name("Self-Help").slug("self-help").build();
        when(categoryMapper.toResponseList(List.of(testCategory))).thenReturn(List.of(catDto));

        // Act
        List<CategoryResponse> result = catalogService.getAllCategories();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Self-Help");
    }

    @Test
    @DisplayName("Should return book details when valid bookId is provided")
    void shouldReturnBookById() {
        // Arrange
        when(bookRepository.findById(testBook.getBookId())).thenReturn(Optional.of(testBook));
        when(bookMapper.toDetailResponse(testBook)).thenReturn(testBookDetail);

        // Act
        BookDetailResponse result = catalogService.getBookById(testBook.getBookId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Atomic Habits");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when bookId does not exist")
    void shouldThrowNotFoundWhenBookDoesNotExist() {
        // Arrange
        UUID unknownId = UUID.randomUUID();
        when(bookRepository.findById(unknownId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> catalogService.getBookById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book");
    }

    @Test
    @DisplayName("Should filter books with pagination")
    void shouldFilterBooksWithPagination() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> bookPage = new PageImpl<>(List.of(testBook), pageable, 1);

        when(bookRepository.findWithFilters(eq("self-help"), any(), any(), any(), any(), eq(pageable)))
                .thenReturn(bookPage);
        when(bookMapper.toCardResponseList(List.of(testBook))).thenReturn(List.of(testBookCard));

        // Act
        PageResponse<BookCardResponse> response = catalogService.getBooks(
                "self-help", null, null, null, null, pageable);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).getTitle()).isEqualTo("Atomic Habits");
    }

    @Test
    @DisplayName("Should search books by keyword")
    void shouldSearchBooksByKeyword() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> bookPage = new PageImpl<>(List.of(testBook), pageable, 1);

        when(bookRepository.searchByKeyword(eq("habits"), eq(pageable))).thenReturn(bookPage);
        when(bookMapper.toCardResponseList(List.of(testBook))).thenReturn(List.of(testBookCard));

        // Act
        PageResponse<BookCardResponse> response = catalogService.searchBooks("habits", pageable);

        // Assert
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getTitle()).isEqualTo("Atomic Habits");
    }
}
