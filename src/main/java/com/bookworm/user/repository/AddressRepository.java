package com.bookworm.user.repository;

import com.bookworm.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByUserUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Address> findByAddressIdAndUserUserId(UUID addressId, UUID userId);

    Optional<Address> findByUserUserIdAndIsDefaultTrue(UUID userId);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.userId = :userId")
    void resetDefaultAddressForUser(@Param("userId") UUID userId);
}
