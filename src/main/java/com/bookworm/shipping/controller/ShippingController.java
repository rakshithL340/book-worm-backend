package com.bookworm.shipping.controller;

import com.bookworm.shipping.dto.EDDResponse;
import com.bookworm.shipping.dto.ShipmentTrackingResponse;
import com.bookworm.shipping.service.ShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequestMapping("/shipping")
@RequiredArgsConstructor
@Tag(name = "Shipping", description = "Tentative delivery date (EDD) estimation and shipment tracking")
public class ShippingController {

    private final ShippingService shippingService;

    @Operation(summary = "Calculate Estimated Delivery Date (EDD)", description = "Computes tentative delivery date based on postal PIN code.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "EDD calculation"),
            @ApiResponse(responseCode = "400", description = "Invalid PIN code")
    })
    @GetMapping("/edd")
    public ResponseEntity<EDDResponse> calculateEdd(
            @Parameter(description = "6-digit Indian Postal PIN Code", example = "560001", required = true)
            @RequestParam("pinCode")
            @Pattern(regexp = "^[1-9][0-9]{5}$", message = "PIN code must be a valid 6-digit Indian postal code")
            String pinCode
    ) {
        log.info("REST request to calculate EDD for PIN code: {}", pinCode);
        EDDResponse response = shippingService.calculateEdd(pinCode);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Track order shipment", description = "Retrieves live shipping milestones and tracking status for an order.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shipment tracking milestones"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    @GetMapping("/track/{orderId}")
    public ResponseEntity<ShipmentTrackingResponse> trackShipmentByOrderId(
            @Parameter(description = "Order UUID", required = true)
            @PathVariable UUID orderId
    ) {
        log.info("REST request to track shipment for order: {}", orderId);
        ShipmentTrackingResponse response = shippingService.trackShipmentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }
}
