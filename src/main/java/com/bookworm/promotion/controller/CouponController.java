package com.bookworm.promotion.controller;

import com.bookworm.common.security.SecurityUtils;
import com.bookworm.promotion.dto.CouponValidateRequest;
import com.bookworm.promotion.dto.CouponValidationResponse;
import com.bookworm.promotion.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Coupons & Loyalty", description = "Promotional coupon validation and loyalty gift points")
public class CouponController {

    private final CouponService couponService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Validate promo coupon code", description = "Verifies discount code eligibility against minimum cart total and validity dates.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coupon validation result"),
            @ApiResponse(responseCode = "400", description = "Validation Failure"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @Valid @RequestBody CouponValidateRequest request
    ) {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to validate coupon: {} for user: {}", request.getCode(), userId);
        CouponValidationResponse response = couponService.validateCoupon(request, userId);
        return ResponseEntity.ok(response);
    }
}
