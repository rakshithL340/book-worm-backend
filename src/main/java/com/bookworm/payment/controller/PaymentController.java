package com.bookworm.payment.controller;

import com.bookworm.common.security.SecurityUtils;
import com.bookworm.payment.dto.PaymentInitiateRequest;
import com.bookworm.payment.dto.PaymentInitiateResponse;
import com.bookworm.payment.dto.WalletBalanceResponse;
import com.bookworm.payment.service.PaymentService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payments", description = "Multi-tender payment settlement, cards, UPI, and digital wallet")
public class PaymentController {

    private final PaymentService paymentService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Initiate and complete payment", description = "Processes payment across Credit Card, Debit Card, UPI, or Wallet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment processed"),
            @ApiResponse(responseCode = "400", description = "Validation Failure / Payment Failure"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiateResponse> initiatePayment(
            @Valid @RequestBody PaymentInitiateRequest request,
            @Parameter(description = "Unique UUID key to guarantee request idempotency")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to process payment: orderId={}, method={}, amount={}, idempotencyKey={}",
                request.getOrderId(), request.getPaymentMethod(), request.getAmount(), idempotencyKey);
        PaymentInitiateResponse response = paymentService.processPayment(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get user wallet balance", description = "Retrieves current digital wallet credit balance for the user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallet balance details"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/wallet")
    public ResponseEntity<WalletBalanceResponse> getWalletBalance() {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to get wallet balance for user: {}", userId);
        WalletBalanceResponse response = paymentService.getWalletBalance(userId);
        return ResponseEntity.ok(response);
    }
}
