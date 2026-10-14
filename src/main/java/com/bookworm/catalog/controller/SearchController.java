package com.bookworm.catalog.controller;

import com.bookworm.catalog.dto.BookCardResponse;
import com.bookworm.catalog.service.CatalogService;
import com.bookworm.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Full-text search across titles, authors, and ISBNs")
public class SearchController {

    private final CatalogService catalogService;

    @Operation(summary = "Full-text search books", description = "Keyword search across book title, author name, and ISBN with category/format filters.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results"),
            @ApiResponse(responseCode = "400", description = "Bad Request")
    })
    @GetMapping
    public ResponseEntity<PageResponse<BookCardResponse>> searchBooks(
            @Parameter(description = "Search keyword", required = true)
            @RequestParam("q") @NotBlank(message = "Search query cannot be blank") @Size(min = 1, max = 100, message = "Search query must be between 1 and 100 characters") String query,
            @Parameter(description = "Category filter")
            @RequestParam(required = false) String category,
            @Parameter(description = "Book format filter")
            @RequestParam(required = false) String format,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "12") int size
    ) {
        log.info("REST request to search books: query='{}', category={}, format={}, page={}, size={}",
                query, category, format, page, size);
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<BookCardResponse> results = catalogService.searchBooks(query, pageable);
        return ResponseEntity.ok(results);
    }
}
