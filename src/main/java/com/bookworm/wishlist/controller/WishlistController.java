package com.bookworm.wishlist.controller;

import com.bookworm.common.security.SecurityUtils;
import com.bookworm.wishlist.dto.WishlistResponse;
import com.bookworm.wishlist.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Wishlist", description = "Customer book wishlists")
public class WishlistController {

    private final WishlistService wishlistService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Get customer wishlist", description = "Returns active wishlist items for the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer wishlist"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<WishlistResponse> getWishlist() {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to get wishlist for user: {}", userId);
        WishlistResponse response = wishlistService.getWishlist(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Add book to wishlist", description = "Adds a book to customer wishlist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Book added to wishlist"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @PostMapping("/{bookId}")
    public ResponseEntity<WishlistResponse> addToWishlist(
            @Parameter(description = "Book UUID", required = true)
            @PathVariable UUID bookId
    ) {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to add book: {} to wishlist for user: {}", bookId, userId);
        WishlistResponse response = wishlistService.addToWishlist(userId, bookId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Remove book from wishlist", description = "Removes a book from customer wishlist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Book removed from wishlist"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> removeFromWishlist(
            @Parameter(description = "Book UUID", required = true)
            @PathVariable UUID bookId
    ) {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to remove book: {} from wishlist for user: {}", bookId, userId);
        wishlistService.removeFromWishlist(userId, bookId);
        return ResponseEntity.noContent().build();
    }
}
