package com.bookworm.promotion.controller;

import com.bookworm.common.security.SecurityUtils;
import com.bookworm.promotion.dto.LoyaltyPointsResponse;
import com.bookworm.promotion.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/loyalty")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Coupons & Loyalty", description = "Promotional coupon validation and loyalty gift points")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Get loyalty gift points balance", description = "Retrieves current gift points balance and monetary conversion value.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Loyalty points balance"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/points")
    public ResponseEntity<LoyaltyPointsResponse> getLoyaltyPoints() {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to get loyalty points for user: {}", userId);
        LoyaltyPointsResponse response = loyaltyService.getLoyaltyPoints(userId);
        return ResponseEntity.ok(response);
    }
}
