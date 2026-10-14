package com.bookworm.cart.service;

import com.bookworm.cart.dto.AddToCartRequest;
import com.bookworm.cart.dto.CartResponse;
import com.bookworm.cart.dto.UpdateQuantityRequest;
import com.bookworm.cart.entity.Cart;
import com.bookworm.cart.entity.CartItem;
import com.bookworm.cart.mapper.CartMapper;
import com.bookworm.cart.repository.CartItemRepository;
import com.bookworm.cart.repository.CartRepository;
import com.bookworm.catalog.entity.Book;
import com.bookworm.catalog.repository.BookRepository;
import com.bookworm.common.exception.BadRequestException;
import com.bookworm.common.exception.InsufficientStockException;
import com.bookworm.common.exception.ResourceNotFoundException;
import com.bookworm.user.entity.User;
import com.bookworm.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;

    @Override
    @Transactional
    public CartResponse getCart(UUID userId, String sessionToken) {
        log.info("Fetching active cart for user: {} or session: {}", userId, sessionToken);
        Cart cart = getOrCreateCart(userId, sessionToken);
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(UUID userId, String sessionToken, AddToCartRequest request) {
        log.info("Adding book {} with quantity {} to cart for user: {} / session: {}",
                request.getBookId(), request.getQuantity(), userId, sessionToken);

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book", "bookId", request.getBookId()));

        Cart cart = getOrCreateCart(userId, sessionToken);

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartCartIdAndBookBookId(
                cart.getCartId(), book.getBookId());

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + request.getQuantity();

            if (book.getStockQuantity() < newQuantity) {
                log.warn("Insufficient stock for book {}. Available: {}, Requested: {}",
                        book.getTitle(), book.getStockQuantity(), newQuantity);
                throw new InsufficientStockException(
                        "Cannot add " + request.getQuantity() + " items. Available stock: " + book.getStockQuantity());
            }

            existingItem.setQuantity(newQuantity);
            existingItem.setUnitPrice(book.getPrice());
            cartItemRepository.save(existingItem);
        } else {
            if (book.getStockQuantity() < request.getQuantity()) {
                log.warn("Insufficient stock for book {}. Available: {}, Requested: {}",
                        book.getTitle(), book.getStockQuantity(), request.getQuantity());
                throw new InsufficientStockException(
                        "Insufficient stock for " + book.getTitle() + ". Available: " + book.getStockQuantity());
            }

            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .book(book)
                    .quantity(request.getQuantity())
                    .unitPrice(book.getPrice())
                    .build();
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
        }

        Cart updatedCart = cartRepository.findById(cart.getCartId()).orElse(cart);
        return cartMapper.toResponse(updatedCart);
    }

    @Override
    @Transactional
    public CartResponse updateQuantity(UUID userId, String sessionToken, UUID cartItemId, UpdateQuantityRequest request) {
        log.info("Updating quantity of cart item {} to {} for user: {} / session: {}",
                cartItemId, request.getQuantity(), userId, sessionToken);

        Cart cart = getOrCreateCart(userId, sessionToken);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "cartItemId", cartItemId));

        if (!cartItem.getCart().getCartId().equals(cart.getCartId())) {
            throw new BadRequestException("Cart item does not belong to the current active cart");
        }

        if (request.getQuantity() <= 0) {
            cart.removeItem(cartItem);
            cartItemRepository.delete(cartItem);
            log.info("Removed cart item {} because quantity was 0", cartItemId);
        } else {
            Book book = cartItem.getBook();
            if (book.getStockQuantity() < request.getQuantity()) {
                throw new InsufficientStockException(
                        "Requested quantity " + request.getQuantity() + " exceeds available stock " + book.getStockQuantity());
            }
            cartItem.setQuantity(request.getQuantity());
            cartItem.setUnitPrice(book.getPrice());
            cartItemRepository.save(cartItem);
        }

        Cart updatedCart = cartRepository.findById(cart.getCartId()).orElse(cart);
        return cartMapper.toResponse(updatedCart);
    }

    @Override
    @Transactional
    public CartResponse removeFromCart(UUID userId, String sessionToken, UUID cartItemId) {
        log.info("Removing cart item {} for user: {} / session: {}", cartItemId, userId, sessionToken);

        Cart cart = getOrCreateCart(userId, sessionToken);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "cartItemId", cartItemId));

        if (!cartItem.getCart().getCartId().equals(cart.getCartId())) {
            throw new BadRequestException("Cart item does not belong to current active cart");
        }

        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);

        Cart updatedCart = cartRepository.findById(cart.getCartId()).orElse(cart);
        return cartMapper.toResponse(updatedCart);
    }

    @Override
    @Transactional
    public void clearCart(UUID userId, String sessionToken) {
        log.info("Clearing active cart for user: {} / session: {}", userId, sessionToken);
        Cart cart = getOrCreateCart(userId, sessionToken);
        cartItemRepository.deleteByCartCartId(cart.getCartId());
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void mergeCart(UUID userId, String sessionToken) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            return;
        }

        log.info("Merging guest session cart [{}] into member user [{}] cart", sessionToken, userId);

        Optional<Cart> guestCartOpt = cartRepository.findBySessionTokenAndActiveTrue(sessionToken);
        if (guestCartOpt.isEmpty()) {
            log.info("No active guest cart found for session token: {}", sessionToken);
            return;
        }

        Cart guestCart = guestCartOpt.get();
        if (guestCart.getItems() == null || guestCart.getItems().isEmpty()) {
            guestCart.setActive(false);
            cartRepository.save(guestCart);
            return;
        }

        Cart userCart = getOrCreateCart(userId, null);

        for (CartItem guestItem : new ArrayList<>(guestCart.getItems())) {
            Book book = guestItem.getBook();
            Optional<CartItem> userItemOpt = cartItemRepository.findByCartCartIdAndBookBookId(
                    userCart.getCartId(), book.getBookId());

            if (userItemOpt.isPresent()) {
                CartItem userItem = userItemOpt.get();
                int combinedQuantity = userItem.getQuantity() + guestItem.getQuantity();
                int finalQuantity = Math.min(combinedQuantity, book.getStockQuantity());
                userItem.setQuantity(finalQuantity);
                userItem.setUnitPrice(book.getPrice());
                cartItemRepository.save(userItem);
            } else {
                int itemQuantity = Math.min(guestItem.getQuantity(), book.getStockQuantity());
                if (itemQuantity > 0) {
                    CartItem newItem = CartItem.builder()
                            .cart(userCart)
                            .book(book)
                            .quantity(itemQuantity)
                            .unitPrice(book.getPrice())
                            .build();
                    userCart.addItem(newItem);
                    cartItemRepository.save(newItem);
                }
            }
        }

        // Deactivate guest cart after merging
        guestCart.setActive(false);
        guestCart.setSessionToken(null);
        cartRepository.save(guestCart);
        log.info("Guest cart merged successfully into user cart [{}]", userCart.getCartId());
    }

    private Cart getOrCreateCart(UUID userId, String sessionToken) {
        if (userId != null) {
            return cartRepository.findByUserUserIdAndActiveTrue(userId)
                    .orElseGet(() -> {
                        User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
                        Cart newCart = Cart.builder()
                                .user(user)
                                .active(true)
                                .build();
                        return cartRepository.save(newCart);
                    });
        } else if (sessionToken != null && !sessionToken.trim().isEmpty()) {
            return cartRepository.findBySessionTokenAndActiveTrue(sessionToken)
                    .orElseGet(() -> {
                        Cart newCart = Cart.builder()
                                .sessionToken(sessionToken)
                                .active(true)
                                .build();
                        return cartRepository.save(newCart);
                    });
        } else {
            throw new BadRequestException("Either authenticated userId or guest session token must be provided");
        }
    }
}
