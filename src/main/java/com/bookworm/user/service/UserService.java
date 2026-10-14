package com.bookworm.user.service;

import com.bookworm.user.dto.AddressCreateRequest;
import com.bookworm.user.dto.AddressResponse;
import com.bookworm.user.dto.UserProfileResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserProfileResponse getUserProfile(UUID userId);

    List<AddressResponse> getUserAddresses(UUID userId);

    AddressResponse addAddress(UUID userId, AddressCreateRequest request);

    AddressResponse updateAddress(UUID userId, UUID addressId, AddressCreateRequest request);

    void deleteAddress(UUID userId, UUID addressId);

    void setDefaultAddress(UUID userId, UUID addressId);
}
