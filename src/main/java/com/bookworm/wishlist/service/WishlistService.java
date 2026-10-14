package com.bookworm.wishlist.service;

import com.bookworm.wishlist.dto.WishlistResponse;

import java.util.UUID;

public interface WishlistService {

    WishlistResponse getWishlist(UUID userId);

    WishlistResponse addToWishlist(UUID userId, UUID bookId);

    WishlistResponse removeFromWishlist(UUID userId, UUID bookId);

    void moveToCart(UUID userId, UUID bookId);
}
