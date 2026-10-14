package com.bookworm.order.controller;

import com.bookworm.cart.dto.CartResponse;
import com.bookworm.cart.service.CartService;
import com.bookworm.common.dto.PageResponse;
import com.bookworm.common.security.SecurityUtils;
import com.bookworm.order.dto.CancelOrderRequest;
import com.bookworm.order.dto.CancelOrderResponse;
import com.bookworm.order.dto.OrderCalculateRequest;
import com.bookworm.order.dto.OrderCheckoutRequest;
import com.bookworm.order.dto.OrderCheckoutResponse;
import com.bookworm.order.dto.OrderDetailResponse;
import com.bookworm.order.dto.OrderPriceCalculationResponse;
import com.bookworm.order.dto.OrderSummaryResponse;
import com.bookworm.order.service.OrderService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders", description = "Checkout calculation, order placement, order history, 48-hour cancellation, and 'Buy It Again'")
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Pre-checkout order pricing calculation", description = "Computes item subtotal, tax amount, delivery charge, coupon discount, and total payable before placing order.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Calculated order breakdown"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/calculate")
    public ResponseEntity<OrderPriceCalculationResponse> calculateOrderTotal(
            @Valid @RequestBody OrderCalculateRequest request
    ) {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to calculate order price for user: {}", userId);
        OrderPriceCalculationResponse response = orderService.calculatePrice(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Place order (Checkout)", description = "Creates a new pending order from active cart, applies coupon/points, and returns order summary.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request / Insufficient stock"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/checkout")
    public ResponseEntity<OrderCheckoutResponse> checkoutOrder(
            @Valid @RequestBody OrderCheckoutRequest request,
            @Parameter(description = "Unique UUID key to guarantee request idempotency")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to checkout order for user: {}, idempotencyKey: {}", userId, idempotencyKey);
        OrderCheckoutResponse response = orderService.checkout(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List order history (My Orders)", description = "Retrieves paginated order history for the authenticated customer.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated order history"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<PageResponse<OrderSummaryResponse>> listOrders(
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size
    ) {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to list orders for user: {}, page={}, size={}", userId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<OrderSummaryResponse> response = orderService.getOrders(userId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get order details", description = "Retrieves complete details of a specific order including items and shipping status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order details"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrderById(
            @Parameter(description = "Order UUID", required = true)
            @PathVariable UUID orderId
    ) {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to get order details: {} for user: {}", orderId, userId);
        OrderDetailResponse response = orderService.getOrderById(userId, orderId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Cancel order (48-Hour Cancellation Rule)",
            description = "Cancels order if within the configurable 48-hour creation window and not yet dispatched, triggering an automated refund."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Cancellation window expired or order already dispatched"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<CancelOrderResponse> cancelOrder(
            @Parameter(description = "Order UUID", required = true)
            @PathVariable UUID orderId,
            @RequestBody(required = false) CancelOrderRequest request
    ) {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to cancel order: {} for user: {}", orderId, userId);
        CancelOrderResponse response = orderService.cancelOrder(userId, orderId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Buy It Again (Reorder)", description = "Adds all available items from a historical order into the active cart.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Items added to cart"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PostMapping("/{orderId}/buy-again")
    public ResponseEntity<CartResponse> buyItAgain(
            @Parameter(description = "Order UUID", required = true)
            @PathVariable UUID orderId
    ) {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to reorder items from order: {} for user: {}", orderId, userId);
        orderService.buyAgain(userId, orderId);
        CartResponse updatedCart = cartService.getCart(userId, null);
        return ResponseEntity.ok(updatedCart);
    }
}
