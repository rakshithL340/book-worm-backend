package com.bookworm.cart.repository;

import com.bookworm.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByUserUserIdAndActiveTrue(UUID userId);

    Optional<Cart> findBySessionTokenAndActiveTrue(String sessionToken);

    Optional<Cart> findByCartIdAndActiveTrue(UUID cartId);
}
