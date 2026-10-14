package com.bookworm.catalog.controller;

import com.bookworm.catalog.dto.BookCardResponse;
import com.bookworm.catalog.dto.BookDetailResponse;
import com.bookworm.catalog.service.CatalogService;
import com.bookworm.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
@Tag(name = "Books", description = "Product catalog, multi-faceted filtering, and book details")
public class BookController {

    private final CatalogService catalogService;

    @Operation(summary = "Browse and filter books", description = "Multi-faceted book catalog query filtered by category, format, language, price range, and sort order.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated book list")
    })
    @GetMapping
    public ResponseEntity<PageResponse<BookCardResponse>> listBooks(
            @Parameter(description = "Category slug or name")
            @RequestParam(required = false) String category,
            @Parameter(description = "Book language (e.g. English, Hindi)")
            @RequestParam(required = false) String language,
            @Parameter(description = "Book format (PAPERBACK, HARDCOVER, EBOOK)")
            @RequestParam(required = false) String format,
            @Parameter(description = "Minimum price filter")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price filter")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Sort order (relevance, price_asc, price_desc, rating, newest)")
            @RequestParam(defaultValue = "relevance") String sortBy,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "12") int size
    ) {
        log.info("REST request to list books: category={}, lang={}, format={}, minPrice={}, maxPrice={}, sortBy={}, page={}, size={}",
                category, language, format, minPrice, maxPrice, sortBy, page, size);

        Sort sort = switch (sortBy.toLowerCase()) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "rating" -> Sort.by(Sort.Direction.DESC, "averageRating");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "salesCount");
        };

        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<BookCardResponse> response = catalogService.getBooks(category, language, format, minPrice, maxPrice, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get book details (PDP)", description = "Returns complete product metadata, author bio, publisher, rating, sales count, and delivery date.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book details retrieved"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @GetMapping("/{bookId}")
    public ResponseEntity<BookDetailResponse> getBookById(
            @Parameter(description = "Book UUID", required = true)
            @PathVariable UUID bookId
    ) {
        log.info("REST request to get book details for ID: {}", bookId);
        BookDetailResponse response = catalogService.getBookById(bookId);
        return ResponseEntity.ok(response);
    }
}
