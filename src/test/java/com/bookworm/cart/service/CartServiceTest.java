package com.bookworm.cart.service;

import com.bookworm.cart.dto.AddToCartRequest;
import com.bookworm.cart.dto.CartItemResponse;
import com.bookworm.cart.dto.CartResponse;
import com.bookworm.cart.dto.UpdateQuantityRequest;
import com.bookworm.cart.entity.Cart;
import com.bookworm.cart.entity.CartItem;
import com.bookworm.cart.mapper.CartMapper;
import com.bookworm.cart.repository.CartItemRepository;
import com.bookworm.cart.repository.CartRepository;
import com.bookworm.catalog.entity.Book;
import com.bookworm.catalog.repository.BookRepository;
import com.bookworm.common.exception.InsufficientStockException;
import com.bookworm.user.entity.User;
import com.bookworm.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Unit Tests")
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    private User testUser;
    private Cart testCart;
    private Book testBook;
    private CartItem testCartItem;
    private CartResponse testCartResponse;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        UUID cartItemId = UUID.randomUUID();

        testUser = User.builder().userId(userId).email("user@example.com").build();
        testCart = Cart.builder().cartId(cartId).user(testUser).active(true).items(new ArrayList<>()).build();

        testBook = Book.builder()
                .bookId(bookId)
                .title("Atomic Habits")
                .price(new BigDecimal("599.00"))
                .stockQuantity(10)
                .build();

        testCartItem = CartItem.builder()
                .cartItemId(cartItemId)
                .cart(testCart)
                .book(testBook)
                .quantity(2)
                .unitPrice(new BigDecimal("599.00"))
                .build();

        testCartResponse = CartResponse.builder()
                .cartId(cartId)
                .totalItems(2)
                .subtotal(new BigDecimal("1198.00"))
                .items(List.of(CartItemResponse.builder()
                        .cartItemId(cartItemId)
                        .bookId(bookId)
                        .title("Atomic Habits")
                        .quantity(2)
                        .unitPrice(new BigDecimal("599.00"))
                        .lineTotal(new BigDecimal("1198.00"))
                        .build()))
                .build();
    }

    @Nested
    @DisplayName("Add To Cart Tests")
    class AddToCartTests {

        @Test
        @DisplayName("Should successfully add a new book item to user cart")
        void shouldAddNewItemToCartSuccessfully() {
            // Arrange
            AddToCartRequest request = AddToCartRequest.builder()
                    .bookId(testBook.getBookId())
                    .quantity(2)
                    .build();

            when(bookRepository.findById(testBook.getBookId())).thenReturn(Optional.of(testBook));
            when(cartRepository.findByUserUserIdAndActiveTrue(testUser.getUserId())).thenReturn(Optional.of(testCart));
            when(cartItemRepository.findByCartCartIdAndBookBookId(testCart.getCartId(), testBook.getBookId()))
                    .thenReturn(Optional.empty());
            when(cartRepository.findById(testCart.getCartId())).thenReturn(Optional.of(testCart));
            when(cartMapper.toResponse(any())).thenReturn(testCartResponse);

            // Act
            CartResponse response = cartService.addToCart(testUser.getUserId(), null, request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getTotalItems()).isEqualTo(2);
            verify(cartItemRepository).save(any(CartItem.class));
        }

        @Test
        @DisplayName("Should throw InsufficientStockException when requested quantity exceeds book stock")
        void shouldThrowInsufficientStockException() {
            // Arrange
            AddToCartRequest request = AddToCartRequest.builder()
                    .bookId(testBook.getBookId())
                    .quantity(15) // stock is only 10
                    .build();

            when(bookRepository.findById(testBook.getBookId())).thenReturn(Optional.of(testBook));
            when(cartRepository.findByUserUserIdAndActiveTrue(testUser.getUserId())).thenReturn(Optional.of(testCart));
            when(cartItemRepository.findByCartCartIdAndBookBookId(testCart.getCartId(), testBook.getBookId()))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cartService.addToCart(testUser.getUserId(), null, request))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Insufficient stock");
        }
    }

    @Nested
    @DisplayName("Quantity Stepper Tests")
    class QuantityStepperTests {

        @Test
        @DisplayName("Should update quantity when within stock limits")
        void shouldUpdateQuantitySuccessfully() {
            // Arrange
            UpdateQuantityRequest request = UpdateQuantityRequest.builder().quantity(5).build();

            when(cartRepository.findByUserUserIdAndActiveTrue(testUser.getUserId())).thenReturn(Optional.of(testCart));
            when(cartItemRepository.findById(testCartItem.getCartItemId())).thenReturn(Optional.of(testCartItem));
            when(cartRepository.findById(testCart.getCartId())).thenReturn(Optional.of(testCart));
            when(cartMapper.toResponse(any())).thenReturn(testCartResponse);

            // Act
            CartResponse response = cartService.updateQuantity(
                    testUser.getUserId(), null, testCartItem.getCartItemId(), request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(testCartItem.getQuantity()).isEqualTo(5);
            verify(cartItemRepository).save(testCartItem);
        }

        @Test
        @DisplayName("Should remove item when updated quantity is zero")
        void shouldRemoveItemWhenQuantityIsZero() {
            // Arrange
            UpdateQuantityRequest request = UpdateQuantityRequest.builder().quantity(0).build();

            when(cartRepository.findByUserUserIdAndActiveTrue(testUser.getUserId())).thenReturn(Optional.of(testCart));
            when(cartItemRepository.findById(testCartItem.getCartItemId())).thenReturn(Optional.of(testCartItem));
            when(cartRepository.findById(testCart.getCartId())).thenReturn(Optional.of(testCart));
            when(cartMapper.toResponse(any())).thenReturn(testCartResponse);

            // Act
            cartService.updateQuantity(testUser.getUserId(), null, testCartItem.getCartItemId(), request);

            // Assert
            verify(cartItemRepository).delete(testCartItem);
        }
    }
}
