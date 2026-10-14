package com.bookworm.common.exception;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_DOCS_BASE_URI = "https://api.bookworm.example.com/errors/";

    // -------------------------------------------------------------------------
    // 1. DOMAIN BUSINESS EXCEPTIONS
    // -------------------------------------------------------------------------

    @ExceptionHandler(BaseBusinessException.class)
    public ResponseEntity<ApiProblemDetail> handleBaseBusinessException(
            BaseBusinessException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        log.warn("[{}] Business exception occurred: {} - {}", traceId, ex.getErrorCode().getCode(), ex.getMessage());

        ApiProblemDetail problem = ApiProblemDetail.builder()
                .timestamp(Instant.now())
                .status(ex.getHttpStatus().value())
                .errorCode(ex.getErrorCode().getCode())
                .title(ex.getErrorCode().name())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .type(URI.create(ERROR_DOCS_BASE_URI + ex.getErrorCode().getCode().toLowerCase().replace('_', '-')))
                .traceId(traceId)
                .build();

        return ResponseEntity
                .status(ex.getHttpStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    // -------------------------------------------------------------------------
    // 2. VALIDATION EXCEPTIONS (RequestBody DTOs & RequestParams)
    // -------------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        log.warn("[{}] DTO Validation failed on URI: {}", traceId, request.getRequestURI());

        List<FieldErrorDto> fieldErrors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(FieldErrorDto.builder()
                    .field(error.getField())
                    .rejectedValue(error.getRejectedValue())
                    .message(error.getDefaultMessage())
                    .build());
        }

        ApiProblemDetail problem = ApiProblemDetail.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                .title("Validation Failed")
                .message("Request payload validation failed for " + fieldErrors.size() + " field(s)")
                .path(request.getRequestURI())
                .type(URI.create(ERROR_DOCS_BASE_URI + "validation-failed"))
                .traceId(traceId)
                .errors(fieldErrors)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        log.warn("[{}] Constraint violation on URI: {}", traceId, request.getRequestURI());

        List<FieldErrorDto> fieldErrors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            String field = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
            fieldErrors.add(FieldErrorDto.builder()
                    .field(field)
                    .rejectedValue(violation.getInvalidValue())
                    .message(violation.getMessage())
                    .build());
        }

        ApiProblemDetail problem = ApiProblemDetail.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                .title("Constraint Violation")
                .message("Request parameter constraint validation failed")
                .path(request.getRequestURI())
                .type(URI.create(ERROR_DOCS_BASE_URI + "constraint-violation"))
                .traceId(traceId)
                .errors(fieldErrors)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    // -------------------------------------------------------------------------
    // 3. HTTP MESSAGE & PARAMETER PARSING EXCEPTIONS
    // -------------------------------------------------------------------------

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiProblemDetail> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        log.warn("[{}] Malformed JSON request body on URI: {}", traceId, request.getRequestURI());

        ApiProblemDetail problem = ApiProblemDetail.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.MALFORMED_JSON.getCode())
                .title("Malformed JSON")
                .message("Failed to parse request JSON body: invalid syntax or incompatible data types")
                .path(request.getRequestURI())
                .type(URI.create(ERROR_DOCS_BASE_URI + "malformed-json"))
                .traceId(traceId)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiProblemDetail> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        log.warn("[{}] Missing required parameter '{}' on URI: {}", traceId, ex.getParameterName(), request.getRequestURI());

        ApiProblemDetail problem = ApiProblemDetail.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.INVALID_PARAMETER.getCode())
                .title("Missing Required Parameter")
                .message(String.format("Required request parameter '%s' of type %s is missing",
                        ex.getParameterName(), ex.getParameterType()))
                .path(request.getRequestURI())
                .type(URI.create(ERROR_DOCS_BASE_URI + "missing-parameter"))
                .traceId(traceId)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiProblemDetail> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        String paramName = ex.getName();
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";

        log.warn("[{}] Type mismatch for parameter '{}' on URI: {}", traceId, paramName, request.getRequestURI());

        ApiProblemDetail problem = ApiProblemDetail.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.INVALID_PARAMETER.getCode())
                .title("Invalid Parameter Type")
                .message(String.format("Parameter '%s' should be of type %s", paramName, requiredType))
                .path(request.getRequestURI())
                .type(URI.create(ERROR_DOCS_BASE_URI + "type-mismatch"))
                .traceId(traceId)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    // -------------------------------------------------------------------------
    // 4. SECURITY & AUTHENTICATION EXCEPTIONS
    // -------------------------------------------------------------------------

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiProblemDetail> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        log.warn("[{}] Invalid credentials for URI: {}", traceId, request.getRequestURI());

        ApiProblemDetail problem = ApiProblemDetail.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .errorCode(ErrorCode.INVALID_CREDENTIALS.getCode())
                .title("Authentication Failed")
                .message("Invalid email or password")
                .path(request.getRequestURI())
                .type(URI.create(ERROR_DOCS_BASE_URI + "invalid-credentials"))
                .traceId(traceId)
                .build();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiProblemDetail> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        log.warn("[{}] Authentication exception for URI: {} - {}", traceId, request.getRequestURI(), ex.getMessage());

        ApiProblemDetail problem = ApiProblemDetail.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .errorCode(ErrorCode.UNAUTHORIZED.getCode())
                .title("Unauthorized")
                .message("Authentication token is missing, expired, or invalid")
                .path(request.getRequestURI())
                .type(URI.create(ERROR_DOCS_BASE_URI + "unauthorized"))
                .traceId(traceId)
                .build();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiProblemDetail> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        log.warn("[{}] Access denied for URI: {} - {}", traceId, request.getRequestURI(), ex.getMessage());

        ApiProblemDetail problem = ApiProblemDetail.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .errorCode(ErrorCode.ACCESS_DENIED.getCode())
                .title("Forbidden")
                .message("Access is denied: insufficient role privileges for this resource")
                .path(request.getRequestURI())
                .type(URI.create(ERROR_DOCS_BASE_URI + "access-denied"))
                .traceId(traceId)
                .build();

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    // -------------------------------------------------------------------------
    // 5. DATABASE, CONCURRENCY & JPA EXCEPTIONS
    // -------------------------------------------------------------------------

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiProblemDetail> handleOptimisticLocking(
            OptimisticLockingFailureException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        log.warn("[{}] Optimistic locking failure on URI: {}", traceId, request.getRequestURI());

        ApiProblemDetail problem = ApiProblemDetail.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.CONFLICT.value())
                .errorCode(ErrorCode.CONCURRENT_MODIFICATION.getCode())
                .title("Concurrent Modification")
                .message("The resource was modified by another transaction. Please reload and try again.")
                .path(request.getRequestURI())
                .type(URI.create(ERROR_DOCS_BASE_URI + "concurrent-modification"))
                .traceId(traceId)
                .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiProblemDetail> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        log.error("[{}] Database constraint violation on URI: {}", traceId, request.getRequestURI(), ex);

        ApiProblemDetail problem = ApiProblemDetail.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.CONFLICT.value())
                .errorCode(ErrorCode.CONFLICT.getCode())
                .title("Data Integrity Conflict")
                .message("Database constraint violation occurred (e.g. duplicate key or foreign key violation)")
                .path(request.getRequestURI())
                .type(URI.create(ERROR_DOCS_BASE_URI + "data-conflict"))
                .traceId(traceId)
                .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    // -------------------------------------------------------------------------
    // 6. ROUTING & HTTP METHOD EXCEPTIONS
    // -------------------------------------------------------------------------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiProblemDetail> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        log.warn("[{}] Method not supported: {} on URI: {}", traceId, ex.getMethod(), request.getRequestURI());

        ApiProblemDetail problem = ApiProblemDetail.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .errorCode(ErrorCode.METHOD_NOT_ALLOWED.getCode())
                .title("Method Not Allowed")
                .message(String.format("HTTP method '%s' is not supported for this endpoint. Supported methods: %s",
                        ex.getMethod(), ex.getSupportedHttpMethods()))
                .path(request.getRequestURI())
                .type(URI.create(ERROR_DOCS_BASE_URI + "method-not-allowed"))
                .traceId(traceId)
                .build();

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiProblemDetail> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        log.warn("[{}] Media type not supported: {} on URI: {}", traceId, ex.getContentType(), request.getRequestURI());

        ApiProblemDetail problem = ApiProblemDetail.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .errorCode(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode())
                .title("Unsupported Media Type")
                .message(String.format("Content type '%s' is not supported. Supported media types: %s",
                        ex.getContentType(), ex.getSupportedMediaTypes()))
                .path(request.getRequestURI())
                .type(URI.create(ERROR_DOCS_BASE_URI + "unsupported-media-type"))
                .traceId(traceId)
                .build();

        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiProblemDetail> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        log.warn("[{}] No static/endpoint resource found for URI: {}", traceId, request.getRequestURI());

        ApiProblemDetail problem = ApiProblemDetail.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .errorCode(ErrorCode.RESOURCE_NOT_FOUND.getCode())
                .title("Resource Not Found")
                .message("No endpoint or static resource found for URI: " + request.getRequestURI())
                .path(request.getRequestURI())
                .type(URI.create(ERROR_DOCS_BASE_URI + "not-found"))
                .traceId(traceId)
                .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    // -------------------------------------------------------------------------
    // 7. GENERIC INTERNAL SERVER ERROR FALLBACK
    // -------------------------------------------------------------------------

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiProblemDetail> handleGenericThrowable(
            Throwable ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        log.error("[{}] Unhandled server exception on URI: {} - Error: {}",
                traceId, request.getRequestURI(), ex.getMessage(), ex);

        ApiProblemDetail problem = ApiProblemDetail.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .title("Internal Server Error")
                .message("An unexpected internal server error occurred. Please quote trace ID " + traceId + " to support.")
                .path(request.getRequestURI())
                .type(URI.create(ERROR_DOCS_BASE_URI + "internal-server-error"))
                .traceId(traceId)
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    // -------------------------------------------------------------------------
    // HELPER METHODS
    // -------------------------------------------------------------------------

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Correlation-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = request.getHeader("X-Request-Id");
        }
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().substring(0, 8);
        }
        return traceId;
    }
}
