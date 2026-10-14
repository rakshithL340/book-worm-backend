package com.bookworm.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request / Validation
    BAD_REQUEST("BAD_REQUEST", HttpStatus.BAD_REQUEST, "The request was invalid or cannot be served"),
    VALIDATION_FAILED("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "Request validation failed for one or more fields"),
    INVALID_PARAMETER("INVALID_PARAMETER", HttpStatus.BAD_REQUEST, "Invalid request parameter supplied"),
    MALFORMED_JSON("MALFORMED_JSON", HttpStatus.BAD_REQUEST, "Malformed JSON request body"),
    
    // 401 & 403 Authentication & Authorization
    UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Full authentication is required to access this resource"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid username or password"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "Authentication token has expired"),
    ACCESS_DENIED("ACCESS_DENIED", HttpStatus.FORBIDDEN, "Access is denied: insufficient permissions"),

    // 404 Not Found
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "The requested resource was not found"),
    USER_NOT_FOUND("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User profile not found"),
    BOOK_NOT_FOUND("BOOK_NOT_FOUND", HttpStatus.NOT_FOUND, "Book not found in catalog"),
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND, "Order record not found"),
    CART_NOT_FOUND("CART_NOT_FOUND", HttpStatus.NOT_FOUND, "Shopping cart not found"),

    // 409 Conflict
    CONFLICT("CONFLICT", HttpStatus.CONFLICT, "Resource conflict occurred"),
    DUPLICATE_RESOURCE("DUPLICATE_RESOURCE", HttpStatus.CONFLICT, "Resource already exists"),
    CONCURRENT_MODIFICATION("CONCURRENT_MODIFICATION", HttpStatus.CONFLICT, "Concurrent modification conflict detected"),

    // Business Invariant Violations (Domain Specific)
    ORDER_NOT_CANCELLABLE("ORDER_NOT_CANCELLABLE", HttpStatus.BAD_REQUEST, "Order cannot be cancelled outside the allowed window or in its current status"),
    INSUFFICIENT_STOCK("INSUFFICIENT_STOCK", HttpStatus.BAD_REQUEST, "Insufficient inventory stock available"),
    INVALID_COUPON("INVALID_COUPON", HttpStatus.BAD_REQUEST, "Coupon code is invalid, expired, or does not meet criteria"),
    INSUFFICIENT_WALLET_BALANCE("INSUFFICIENT_WALLET_BALANCE", HttpStatus.BAD_REQUEST, "Insufficient wallet balance to complete transaction"),
    PAYMENT_FAILED("PAYMENT_FAILED", HttpStatus.BAD_REQUEST, "Payment processing transaction failed"),
    INVALID_ADDRESS("INVALID_ADDRESS", HttpStatus.BAD_REQUEST, "Delivery address is invalid or unserviceable"),

    // 405, 415, 429 Client Errors
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported for this endpoint"),
    UNSUPPORTED_MEDIA_TYPE("UNSUPPORTED_MEDIA_TYPE", HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content type is not supported"),
    RATE_LIMIT_EXCEEDED("RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Please try again later"),

    // 500 Internal Server Errors
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred"),
    DATABASE_ERROR("DATABASE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Database transaction error occurred");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
