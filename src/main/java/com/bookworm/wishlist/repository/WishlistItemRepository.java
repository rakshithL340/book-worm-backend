package com.bookworm.wishlist.repository;

import com.bookworm.wishlist.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {

    List<WishlistItem> findByWishlistWishlistIdOrderByAddedAtDesc(UUID wishlistId);

    Optional<WishlistItem> findByWishlistWishlistIdAndBookBookId(UUID wishlistId, UUID bookId);

    boolean existsByWishlistWishlistIdAndBookBookId(UUID wishlistId, UUID bookId);

    void deleteByWishlistWishlistIdAndBookBookId(UUID wishlistId, UUID bookId);
}
