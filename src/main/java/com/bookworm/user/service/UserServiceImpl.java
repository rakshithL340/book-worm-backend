package com.bookworm.user.service;

import com.bookworm.common.exception.ResourceNotFoundException;
import com.bookworm.user.dto.AddressCreateRequest;
import com.bookworm.user.dto.AddressResponse;
import com.bookworm.user.dto.UserProfileResponse;
import com.bookworm.user.entity.Address;
import com.bookworm.user.entity.User;
import com.bookworm.user.mapper.AddressMapper;
import com.bookworm.user.mapper.UserMapper;
import com.bookworm.user.repository.AddressRepository;
import com.bookworm.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(UUID userId) {
        log.info("Fetching profile for user: {}", userId);
        User user = findUserById(userId);
        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(UUID userId) {
        log.info("Fetching addresses for user: {}", userId);
        findUserById(userId);
        List<Address> addresses = addressRepository.findByUserUserId(userId);
        return addressMapper.toResponseList(addresses);
    }

    @Override
    @Transactional
    public AddressResponse addAddress(UUID userId, AddressCreateRequest request) {
        log.info("Adding new address for user: {}", userId);
        User user = findUserById(userId);

        Address address = addressMapper.toEntity(request);
        address.setUser(user);

        // If this is set as default or is the first address, ensure others are non-default
        List<Address> existingAddresses = addressRepository.findByUserUserId(userId);
        if (existingAddresses.isEmpty() || Boolean.TRUE.equals(request.getIsDefault())) {
            address.setDefault(true);
            existingAddresses.forEach(a -> a.setDefault(false));
            addressRepository.saveAll(existingAddresses);
        }

        Address savedAddress = addressRepository.save(address);
        log.info("Saved address with ID: {} for user: {}", savedAddress.getAddressId(), userId);
        return addressMapper.toResponse(savedAddress);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(UUID userId, UUID addressId, AddressCreateRequest request) {
        log.info("Updating address: {} for user: {}", addressId, userId);
        findUserById(userId);

        Address address = addressRepository.findByAddressIdAndUserUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        addressMapper.updateEntityFromRequest(request, address);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            List<Address> otherAddresses = addressRepository.findByUserUserId(userId);
            otherAddresses.stream()
                    .filter(a -> !a.getAddressId().equals(addressId))
                    .forEach(a -> a.setDefault(false));
            addressRepository.saveAll(otherAddresses);
            address.setDefault(true);
        }

        Address updatedAddress = addressRepository.save(address);
        return addressMapper.toResponse(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        log.info("Deleting address: {} for user: {}", addressId, userId);
        findUserById(userId);

        Address address = addressRepository.findByAddressIdAndUserUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        addressRepository.delete(address);
        log.info("Successfully deleted address: {}", addressId);
    }

    @Override
    @Transactional
    public void setDefaultAddress(UUID userId, UUID addressId) {
        log.info("Setting default address: {} for user: {}", addressId, userId);
        findUserById(userId);

        List<Address> addresses = addressRepository.findByUserUserId(userId);
        boolean found = false;
        for (Address addr : addresses) {
            if (addr.getAddressId().equals(addressId)) {
                addr.setDefault(true);
                found = true;
            } else {
                addr.setDefault(false);
            }
        }

        if (!found) {
            throw new ResourceNotFoundException("Address", "addressId", addressId);
        }

        addressRepository.saveAll(addresses);
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
    }
}
