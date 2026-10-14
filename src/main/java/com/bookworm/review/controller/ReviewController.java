package com.bookworm.review.controller;

import com.bookworm.common.dto.PageResponse;
import com.bookworm.common.security.SecurityUtils;
import com.bookworm.review.dto.ReviewCreateRequest;
import com.bookworm.review.dto.ReviewResponse;
import com.bookworm.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/books/{bookId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Customer star ratings and 100-character reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "List reviews for a book", description = "Retrieves paginated customer reviews and ratings for a book.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book reviews list"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @GetMapping
    public ResponseEntity<PageResponse<ReviewResponse>> listBookReviews(
            @Parameter(description = "Book UUID", required = true)
            @PathVariable UUID bookId,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("REST request to list reviews for book: {}, page={}, size={}", bookId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ReviewResponse> response = reviewService.getReviewsForBook(bookId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Submit a book review",
            description = "Submits a 1-5 star rating and review comment strictly constrained to a maximum of 100 characters."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Review submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Validation Failure (e.g. comment > 100 characters)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Book not found"),
            @ApiResponse(responseCode = "409", description = "Already reviewed this book")
    })
    @PostMapping
    public ResponseEntity<ReviewResponse> createBookReview(
            @Parameter(description = "Book UUID", required = true)
            @PathVariable UUID bookId,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to submit review for book: {} by user: {}", bookId, userId);
        ReviewResponse response = reviewService.addReview(userId, bookId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
