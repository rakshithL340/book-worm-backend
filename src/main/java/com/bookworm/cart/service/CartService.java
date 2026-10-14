package com.bookworm.cart.service;

import com.bookworm.cart.dto.AddToCartRequest;
import com.bookworm.cart.dto.CartResponse;
import com.bookworm.cart.dto.UpdateQuantityRequest;

import java.util.UUID;

public interface CartService {

    CartResponse getCart(UUID userId, String sessionToken);

    CartResponse addToCart(UUID userId, String sessionToken, AddToCartRequest request);

    CartResponse updateQuantity(UUID userId, String sessionToken, UUID cartItemId, UpdateQuantityRequest request);

    CartResponse removeFromCart(UUID userId, String sessionToken, UUID cartItemId);

    void clearCart(UUID userId, String sessionToken);

    void mergeCart(UUID userId, String sessionToken);
}
