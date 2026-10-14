package com.bookworm.catalog.service;

import com.bookworm.catalog.dto.AuthorDetailResponse;
import com.bookworm.catalog.dto.AuthorSummaryResponse;
import com.bookworm.catalog.dto.BookCardResponse;
import com.bookworm.catalog.dto.BookDetailResponse;
import com.bookworm.catalog.dto.CategoryResponse;
import com.bookworm.catalog.dto.PublisherResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final BookRepository bookRepository;

    private final CategoryMapper categoryMapper;
    private final AuthorMapper authorMapper;
    private final PublisherMapper publisherMapper;
    private final BookMapper bookMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        log.info("Fetching all active catalog categories");
        List<Category> categories = categoryRepository.findAllByOrderByDisplayOrderAsc();
        return categoryMapper.toResponseList(categories);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        log.info("Fetching category by slug: {}", slug);
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuthorSummaryResponse> getAuthors(Pageable pageable) {
        log.info("Fetching paginated authors: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Author> authorsPage = authorRepository.findAll(pageable);
        List<AuthorSummaryResponse> content = authorMapper.toSummaryResponseList(authorsPage.getContent());
        return PageResponse.of(content, authorsPage);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthorDetailResponse getAuthorById(UUID authorId) {
        log.info("Fetching author details for ID: {}", authorId);
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author", "authorId", authorId));
        return authorMapper.toDetailResponse(author);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookCardResponse> getBooksByAuthor(UUID authorId, Pageable pageable) {
        log.info("Fetching books for author ID: {}", authorId);
        if (!authorRepository.existsById(authorId)) {
            throw new ResourceNotFoundException("Author", "authorId", authorId);
        }
        Page<Book> booksPage = bookRepository.findByAuthorAuthorId(authorId, pageable);
        List<BookCardResponse> content = bookMapper.toCardResponseList(booksPage.getContent());
        return PageResponse.of(content, booksPage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublisherResponse> getAllPublishers() {
        log.info("Fetching all book publishers");
        List<Publisher> publishers = publisherRepository.findAll();
        return publisherMapper.toResponseList(publishers);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookCardResponse> getBooks(
            String category,
            String language,
            String format,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    ) {
        log.info("Fetching books with filters: category={}, language={}, format={}, minPrice={}, maxPrice={}",
                category, language, format, minPrice, maxPrice);

        BookFormat bookFormat = null;
        if (format != null && !format.isBlank()) {
            try {
                bookFormat = BookFormat.valueOf(format.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid format provided: {}", format);
            }
        }

        Page<Book> booksPage = bookRepository.findWithFilters(
                (category != null && !category.isBlank()) ? category.trim() : null,
                (language != null && !language.isBlank()) ? language.trim() : null,
                bookFormat,
                minPrice,
                maxPrice,
                pageable
        );

        List<BookCardResponse> content = bookMapper.toCardResponseList(booksPage.getContent());
        return PageResponse.of(content, booksPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookCardResponse> searchBooks(String query, Pageable pageable) {
        log.info("Searching books with query: {}", query);
        if (query == null || query.trim().isEmpty()) {
            return getBooks(null, null, null, null, null, pageable);
        }
        Page<Book> booksPage = bookRepository.searchByKeyword(query.trim(), pageable);
        List<BookCardResponse> content = bookMapper.toCardResponseList(booksPage.getContent());
        return PageResponse.of(content, booksPage);
    }

    @Override
    @Transactional(readOnly = true)
    public BookDetailResponse getBookById(UUID bookId) {
        log.info("Fetching book details for ID: {}", bookId);
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "bookId", bookId));
        return bookMapper.toDetailResponse(book);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookCardResponse> getBestsellers(int limit) {
        log.info("Fetching top {} bestsellers", limit);
        Pageable pageable = PageRequest.of(0, Math.max(1, limit));
        List<Book> bestsellers = bookRepository.findBestsellers(pageable);
        return bookMapper.toCardResponseList(bestsellers);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookCardResponse> getNewArrivals(int limit) {
        log.info("Fetching top {} new arrivals", limit);
        Pageable pageable = PageRequest.of(0, Math.max(1, limit));
        List<Book> newArrivals = bookRepository.findNewLaunches(pageable);
        return bookMapper.toCardResponseList(newArrivals);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookCardResponse> getRelatedBooks(UUID bookId, int limit) {
        log.info("Fetching related books for book ID: {}", bookId);
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "bookId", bookId));

        UUID categoryId = book.getCategory() != null ? book.getCategory().getCategoryId() : null;
        UUID authorId = book.getAuthor() != null ? book.getAuthor().getAuthorId() : null;

        Pageable pageable = PageRequest.of(0, Math.max(1, limit));
        List<Book> related = bookRepository.findRelatedBooks(categoryId, authorId, bookId, pageable);
        return bookMapper.toCardResponseList(related);
    }
}
