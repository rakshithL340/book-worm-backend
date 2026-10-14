package com.bookworm.cart.controller;

import com.bookworm.cart.dto.AddToCartRequest;
import com.bookworm.cart.dto.CartMergeRequest;
import com.bookworm.cart.dto.CartResponse;
import com.bookworm.cart.dto.UpdateQuantityRequest;
import com.bookworm.cart.service.CartService;
import com.bookworm.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart with quantity steppers and guest session merging")
public class CartController {

    private final CartService cartService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Get active shopping cart", description = "Returns cart items and totals for authorized user or guest session token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active cart details")
    })
    @GetMapping
    public ResponseEntity<CartResponse> getActiveCart(
            @Parameter(description = "Guest user session identifier token")
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        UUID userId = securityUtils.getCurrentUserIdOptional();
        log.info("REST request to get active cart: userId={}, sessionToken={}", userId, sessionToken);
        CartResponse response = cartService.getCart(userId, sessionToken);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Add book to cart", description = "Adds a book item with specified quantity into the shopping cart.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart updated with new item"),
            @ApiResponse(responseCode = "400", description = "Validation Failure / Insufficient stock"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItemToCart(
            @Valid @RequestBody AddToCartRequest request,
            @Parameter(description = "Guest user session identifier token")
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        UUID userId = securityUtils.getCurrentUserIdOptional();
        log.info("REST request to add item to cart: bookId={}, qty={}, userId={}, sessionToken={}",
                request.getBookId(), request.getQuantity(), userId, sessionToken);
        CartResponse response = cartService.addToCart(userId, sessionToken, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update cart item quantity (Stepper - 1 +)", description = "Updates quantity for a specific cart line item.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Quantity updated"),
            @ApiResponse(responseCode = "400", description = "Validation Failure / Insufficient stock"),
            @ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateCartItemQuantity(
            @Parameter(description = "Cart item UUID", required = true)
            @PathVariable UUID cartItemId,
            @Valid @RequestBody UpdateQuantityRequest request,
            @Parameter(description = "Guest user session identifier token")
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        UUID userId = securityUtils.getCurrentUserIdOptional();
        log.info("REST request to update cart item quantity: cartItemId={}, qty={}, userId={}, sessionToken={}",
                cartItemId, request.getQuantity(), userId, sessionToken);
        CartResponse response = cartService.updateQuantity(userId, sessionToken, cartItemId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove item from cart", description = "Deletes a line item from the shopping cart.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item removed from cart"),
            @ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeCartItem(
            @Parameter(description = "Cart item UUID", required = true)
            @PathVariable UUID cartItemId,
            @Parameter(description = "Guest user session identifier token")
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        UUID userId = securityUtils.getCurrentUserIdOptional();
        log.info("REST request to remove cart item: cartItemId={}, userId={}, sessionToken={}",
                cartItemId, userId, sessionToken);
        CartResponse response = cartService.removeFromCart(userId, sessionToken, cartItemId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Merge guest cart into member account", description = "Merges guest cart items into authenticated customer account.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Merged cart result"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/merge")
    public ResponseEntity<CartResponse> mergeGuestCart(
            @Valid @RequestBody CartMergeRequest request
    ) {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to merge guest cart: sessionToken={} for user: {}", request.getSessionToken(), userId);
        cartService.mergeCart(userId, request.getSessionToken());
        CartResponse response = cartService.getCart(userId, null);
        return ResponseEntity.ok(response);
    }
}
