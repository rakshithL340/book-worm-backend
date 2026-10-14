package com.bookworm.catalog.controller;

import com.bookworm.catalog.dto.BookCardResponse;
import com.bookworm.catalog.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Curated home shelves and product detail cross-sell recommendations")
public class RecommendationController {

    private final CatalogService catalogService;

    @Operation(summary = "Personalized recommendations", description = "Returns 'Recommended for You' books based on order history or trending titles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recommended books")
    })
    @GetMapping("/for-you")
    public ResponseEntity<List<BookCardResponse>> getRecommendationsForYou(
            @Parameter(description = "Maximum number of books to return")
            @RequestParam(defaultValue = "6") int limit
    ) {
        log.info("REST request for 'Recommended for you' with limit: {}", limit);
        List<BookCardResponse> books = catalogService.getBestsellers(Math.min(limit, 20));
        return ResponseEntity.ok(books);
    }

    @Operation(summary = "Monthly bestsellers", description = "Returns 'Bestsellers this Month' sorted by sales velocity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bestseller books")
    })
    @GetMapping("/bestsellers")
    public ResponseEntity<List<BookCardResponse>> getBestsellers(
            @Parameter(description = "Maximum number of books to return")
            @RequestParam(defaultValue = "6") int limit
    ) {
        log.info("REST request for bestsellers with limit: {}", limit);
        List<BookCardResponse> books = catalogService.getBestsellers(Math.min(limit, 20));
        return ResponseEntity.ok(books);
    }

    @Operation(summary = "New book launches", description = "Returns newly cataloged book releases.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "New launch books")
    })
    @GetMapping("/new-launches")
    public ResponseEntity<List<BookCardResponse>> getNewLaunches(
            @Parameter(description = "Maximum number of books to return")
            @RequestParam(defaultValue = "6") int limit
    ) {
        log.info("REST request for new launches with limit: {}", limit);
        List<BookCardResponse> books = catalogService.getNewArrivals(Math.min(limit, 20));
        return ResponseEntity.ok(books);
    }

    @Operation(summary = "Related reads (PDP cross-sell)", description = "Returns related books for a specific title.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Related books list"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @GetMapping("/related/{bookId}")
    public ResponseEntity<List<BookCardResponse>> getRelatedReads(
            @Parameter(description = "Book UUID", required = true)
            @PathVariable UUID bookId,
            @Parameter(description = "Maximum number of books to return")
            @RequestParam(defaultValue = "3") int limit
    ) {
        log.info("REST request for related books of book: {} with limit: {}", bookId, limit);
        List<BookCardResponse> books = catalogService.getRelatedBooks(bookId, Math.min(limit, 10));
        return ResponseEntity.ok(books);
    }
}
