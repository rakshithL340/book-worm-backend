package com.bookworm.wishlist.service;

import com.bookworm.cart.dto.AddToCartRequest;
import com.bookworm.cart.service.CartService;
import com.bookworm.catalog.entity.Book;
import com.bookworm.catalog.repository.BookRepository;
import com.bookworm.common.exception.ResourceNotFoundException;
import com.bookworm.user.entity.User;
import com.bookworm.user.repository.UserRepository;
import com.bookworm.wishlist.dto.WishlistResponse;
import com.bookworm.wishlist.entity.Wishlist;
import com.bookworm.wishlist.entity.WishlistItem;
import com.bookworm.wishlist.mapper.WishlistMapper;
import com.bookworm.wishlist.repository.WishlistItemRepository;
import com.bookworm.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final WishlistMapper wishlistMapper;
    private final CartService cartService;

    @Override
    @Transactional
    public WishlistResponse getWishlist(UUID userId) {
        log.info("Fetching wishlist for user: {}", userId);
        Wishlist wishlist = getOrCreateWishlist(userId);
        return wishlistMapper.toResponse(wishlist);
    }

    @Override
    @Transactional
    public WishlistResponse addToWishlist(UUID userId, UUID bookId) {
        log.info("Adding book {} to wishlist for user: {}", bookId, userId);
        Wishlist wishlist = getOrCreateWishlist(userId);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "bookId", bookId));

        boolean exists = wishlistItemRepository.existsByWishlistWishlistIdAndBookBookId(wishlist.getWishlistId(), bookId);
        if (!exists) {
            WishlistItem item = WishlistItem.builder()
                    .wishlist(wishlist)
                    .book(book)
                    .addedAt(Instant.now())
                    .build();
            wishlist.addItem(item);
            wishlistRepository.save(wishlist);
            log.info("Added book {} to wishlist for user {}", bookId, userId);
        } else {
            log.info("Book {} is already in wishlist for user {}", bookId, userId);
        }

        return wishlistMapper.toResponse(wishlist);
    }

    @Override
    @Transactional
    public WishlistResponse removeFromWishlist(UUID userId, UUID bookId) {
        log.info("Removing book {} from wishlist for user: {}", bookId, userId);
        Wishlist wishlist = getOrCreateWishlist(userId);

        wishlistItemRepository.findByWishlistWishlistIdAndBookBookId(wishlist.getWishlistId(), bookId)
                .ifPresent(item -> {
                    wishlist.removeItem(item);
                    wishlistItemRepository.delete(item);
                    log.info("Removed book {} from wishlist for user {}", bookId, userId);
                });

        return wishlistMapper.toResponse(wishlist);
    }

    @Override
    @Transactional
    public void moveToCart(UUID userId, UUID bookId) {
        log.info("Moving book {} from wishlist to cart for user: {}", bookId, userId);
        Wishlist wishlist = getOrCreateWishlist(userId);

        // Add to cart with quantity 1
        AddToCartRequest addToCartRequest = AddToCartRequest.builder()
                .bookId(bookId)
                .quantity(1)
                .build();
        cartService.addToCart(userId, null, addToCartRequest);

        // Remove from wishlist
        wishlistItemRepository.findByWishlistWishlistIdAndBookBookId(wishlist.getWishlistId(), bookId)
                .ifPresent(item -> {
                    wishlist.removeItem(item);
                    wishlistItemRepository.delete(item);
                    log.info("Removed book {} from wishlist after moving to cart", bookId);
                });
    }

    private Wishlist getOrCreateWishlist(UUID userId) {
        return wishlistRepository.findByUserUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
                    Wishlist newWishlist = Wishlist.builder()
                            .user(user)
                            .build();
                    return wishlistRepository.save(newWishlist);
                });
    }
}
