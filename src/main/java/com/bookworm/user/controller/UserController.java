package com.bookworm.user.controller;

import com.bookworm.common.security.SecurityUtils;
import com.bookworm.user.dto.AddressCreateRequest;
import com.bookworm.user.dto.AddressResponse;
import com.bookworm.user.dto.UserProfileResponse;
import com.bookworm.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users & Addresses", description = "Customer profile and delivery address book management")
public class UserController {

    private final UserService userService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Get current user profile", description = "Retrieves profile information for the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile() {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to fetch profile for user: {}", userId);
        UserProfileResponse response = userService.getUserProfile(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List saved delivery addresses", description = "Returns all delivery addresses for the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of saved delivery addresses"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/addresses")
    public ResponseEntity<List<AddressResponse>> listUserAddresses() {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to list addresses for user: {}", userId);
        List<AddressResponse> addresses = userService.getUserAddresses(userId);
        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Add a new delivery address", description = "Adds a delivery address to the user's address book.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Address created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation Failure"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/addresses")
    public ResponseEntity<AddressResponse> addAddress(@Valid @RequestBody AddressCreateRequest request) {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to add address for user: {}", userId);
        AddressResponse response = userService.addAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update a delivery address", description = "Updates an existing delivery address.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Address updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation Failure"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    @PutMapping("/addresses/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(
            @Parameter(description = "Address UUID", required = true)
            @PathVariable UUID addressId,
            @Valid @RequestBody AddressCreateRequest request
    ) {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to update address: {} for user: {}", addressId, userId);
        AddressResponse response = userService.updateAddress(userId, addressId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a delivery address", description = "Soft deletes a delivery address from the user's address book.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Address deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @Parameter(description = "Address UUID", required = true)
            @PathVariable UUID addressId
    ) {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to delete address: {} for user: {}", addressId, userId);
        userService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Set address as default", description = "Marks a specific delivery address as default.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Address marked as default"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    @PatchMapping("/addresses/{addressId}/default")
    public ResponseEntity<Void> setDefaultAddress(
            @Parameter(description = "Address UUID", required = true)
            @PathVariable UUID addressId
    ) {
        UUID userId = securityUtils.getCurrentUserId();
        log.info("REST request to set default address: {} for user: {}", addressId, userId);
        userService.setDefaultAddress(userId, addressId);
        return ResponseEntity.ok().build();
    }
}
