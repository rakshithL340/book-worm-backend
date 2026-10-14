package com.bookworm.store.repository;

import com.bookworm.store.entity.StorePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StorePolicyRepository extends JpaRepository<StorePolicy, UUID> {

    Optional<StorePolicy> findByStoreStoreId(UUID storeId);

    Optional<StorePolicy> findFirstByOrderByCreatedAtAsc();
}
