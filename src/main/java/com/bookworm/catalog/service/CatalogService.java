package com.bookworm.catalog.service;

import com.bookworm.catalog.dto.AuthorDetailResponse;
import com.bookworm.catalog.dto.AuthorSummaryResponse;
import com.bookworm.catalog.dto.BookCardResponse;
import com.bookworm.catalog.dto.BookDetailResponse;
import com.bookworm.catalog.dto.CategoryResponse;
import com.bookworm.catalog.dto.PublisherResponse;
import com.bookworm.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CatalogService {

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryBySlug(String slug);

    PageResponse<AuthorSummaryResponse> getAuthors(Pageable pageable);

    AuthorDetailResponse getAuthorById(UUID authorId);

    PageResponse<BookCardResponse> getBooksByAuthor(UUID authorId, Pageable pageable);

    List<PublisherResponse> getAllPublishers();

    PageResponse<BookCardResponse> getBooks(
            String category,
            String language,
            String format,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    );

    PageResponse<BookCardResponse> searchBooks(String query, Pageable pageable);

    BookDetailResponse getBookById(UUID bookId);

    List<BookCardResponse> getBestsellers(int limit);

    List<BookCardResponse> getNewArrivals(int limit);

    List<BookCardResponse> getRelatedBooks(UUID bookId, int limit);
}
