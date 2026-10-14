package com.bookworm.store.repository;

import com.bookworm.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreRepository extends JpaRepository<Store, UUID> {

    Optional<Store> findByStoreCode(String storeCode);

    boolean existsByStoreCode(String storeCode);

    Optional<Store> findFirstByActiveTrue();
}
