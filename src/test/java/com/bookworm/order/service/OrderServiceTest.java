package com.bookworm.order.service;

import com.bookworm.cart.entity.Cart;
import com.bookworm.cart.entity.CartItem;
import com.bookworm.cart.repository.CartItemRepository;
import com.bookworm.cart.repository.CartRepository;
import com.bookworm.cart.service.CartService;
import com.bookworm.catalog.entity.Book;
import com.bookworm.catalog.repository.BookRepository;
import com.bookworm.common.exception.OrderNotCancellableException;
import com.bookworm.order.dto.CancelOrderRequest;
import com.bookworm.order.dto.CancelOrderResponse;
import com.bookworm.order.dto.OrderCalculateRequest;
import com.bookworm.order.dto.OrderCheckoutRequest;
import com.bookworm.order.dto.OrderCheckoutResponse;
import com.bookworm.order.dto.OrderPriceCalculationResponse;
import com.bookworm.order.entity.Order;
import com.bookworm.order.entity.OrderItem;
import com.bookworm.order.entity.OrderStatus;
import com.bookworm.order.mapper.OrderMapper;
import com.bookworm.order.repository.OrderItemRepository;
import com.bookworm.order.repository.OrderRepository;
import com.bookworm.order.repository.OrderStatusHistoryRepository;
import com.bookworm.payment.repository.PaymentTransactionRepository;
import com.bookworm.payment.repository.RefundTransactionRepository;
import com.bookworm.promotion.repository.LoyaltyAccountRepository;
import com.bookworm.promotion.service.CouponService;
import com.bookworm.promotion.service.LoyaltyService;
import com.bookworm.store.entity.Store;
import com.bookworm.store.entity.StorePolicy;
import com.bookworm.store.repository.StorePolicyRepository;
import com.bookworm.store.repository.StoreRepository;
import com.bookworm.user.entity.Address;
import com.bookworm.user.entity.User;
import com.bookworm.user.repository.AddressRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private CartService cartService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StorePolicyRepository storePolicyRepository;
    @Mock
    private CouponService couponService;
    @Mock
    private LoyaltyService loyaltyService;
    @Mock
    private LoyaltyAccountRepository loyaltyAccountRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private RefundTransactionRepository refundTransactionRepository;
    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User testUser;
    private Address testAddress;
    private Store testStore;
    private StorePolicy testPolicy;
    private Book testBook;
    private Cart testCart;
    private CartItem testCartItem;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        testUser = User.builder().userId(userId).email("customer@example.com").build();
        testAddress = Address.builder().addressId(addressId).user(testUser).pinCode("560001").build();
        testStore = Store.builder().storeId(storeId).storeCode("BW-01").name("Main Store").build();

        testPolicy = StorePolicy.builder()
                .taxRatePercent(new BigDecimal("12.00"))
                .freeDeliveryThreshold(new BigDecimal("500.00"))
                .standardDeliveryFee(new BigDecimal("50.00"))
                .loyaltyRedemptionRate(new BigDecimal("1.00"))
                .cancellationWindowHours(48)
                .build();

        testBook = Book.builder()
                .bookId(bookId)
                .title("Atomic Habits")
                .price(new BigDecimal("600.00"))
                .stockQuantity(50)
                .salesCount(100L)
                .build();

        testCartItem = CartItem.builder()
                .cartItemId(UUID.randomUUID())
                .book(testBook)
                .quantity(1)
                .unitPrice(new BigDecimal("600.00"))
                .build();

        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(testCartItem);

        testCart = Cart.builder()
                .cartId(cartId)
                .user(testUser)
                .active(true)
                .items(cartItems)
                .build();

        testOrder = Order.builder()
                .orderId(orderId)
                .orderNumber("ORD-12345")
                .user(testUser)
                .store(testStore)
                .shippingAddress(testAddress)
                .status(OrderStatus.CREATED)
                .subtotal(new BigDecimal("600.00"))
                .taxAmount(new BigDecimal("72.00"))
                .deliveryCharge(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("672.00"))
                .tentativeDeliveryDate(LocalDate.now().plusDays(3))
                .createdAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .items(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("Pre-Checkout Calculation Tests")
    class PricingCalculationTests {

        @Test
        @DisplayName("Should compute correct subtotal, 12% tax, and free delivery over ₹500")
        void shouldCalculatePriceWithFreeDelivery() {
            // Arrange
            OrderCalculateRequest request = OrderCalculateRequest.builder()
                    .shippingAddressId(testAddress.getAddressId())
                    .redeemGiftPoints(0)
                    .build();

            when(cartRepository.findByUserUserIdAndActiveTrue(testUser.getUserId())).thenReturn(Optional.of(testCart));
            when(storePolicyRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(testPolicy));

            // Act
            OrderPriceCalculationResponse response = orderService.calculatePrice(testUser.getUserId(), request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getSubtotal()).isEqualByComparingTo("600.00");
            assertThat(response.getTaxAmount()).isEqualByComparingTo("72.00"); // 12% of 600
            assertThat(response.getDeliveryCharge()).isEqualByComparingTo("0.00"); // Free since 600 >= 500
            assertThat(response.isFreeDelivery()).isTrue();
            assertThat(response.getTotalPayableAmount()).isEqualByComparingTo("672.00");
        }
    }

    @Nested
    @DisplayName("Checkout Order Tests")
    class CheckoutOrderTests {

        @Test
        @DisplayName("Should place order, deduct inventory stock, and clear cart")
        void shouldCheckoutSuccessfully() {
            // Arrange
            OrderCheckoutRequest request = OrderCheckoutRequest.builder()
                    .shippingAddressId(testAddress.getAddressId())
                    .build();

            when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
            when(addressRepository.findByAddressIdAndUserUserId(testAddress.getAddressId(), testUser.getUserId()))
                    .thenReturn(Optional.of(testAddress));
            when(cartRepository.findByUserUserIdAndActiveTrue(testUser.getUserId())).thenReturn(Optional.of(testCart));
            when(storeRepository.findFirstByActiveTrue()).thenReturn(Optional.of(testStore));
            when(storePolicyRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(testPolicy));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // Act
            OrderCheckoutResponse response = orderService.checkout(testUser.getUserId(), request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getOrderNumber()).isEqualTo("ORD-12345");
            assertThat(testBook.getStockQuantity()).isEqualTo(49); // Decremented from 50 to 49

            verify(bookRepository).save(testBook);
            verify(cartItemRepository).deleteByCartCartId(testCart.getCartId());
        }
    }

    @Nested
    @DisplayName("48-Hour Order Cancellation Rule Tests")
    class OrderCancellationTests {

        @Test
        @DisplayName("Should cancel order and restore inventory within 48-hour window")
        void shouldCancelOrderWithin48Hours() {
            // Arrange
            CancelOrderRequest request = CancelOrderRequest.builder().reason("Changed mind").build();

            testOrder.setCreatedAt(Instant.now().minus(10, ChronoUnit.HOURS)); // 10h ago <= 48h
            OrderItem orderItem = OrderItem.builder()
                    .book(testBook)
                    .quantity(1)
                    .unitPrice(new BigDecimal("600.00"))
                    .build();
            testOrder.getItems().add(orderItem);

            when(orderRepository.findByOrderIdAndUserUserId(testOrder.getOrderId(), testUser.getUserId()))
                    .thenReturn(Optional.of(testOrder));
            when(storePolicyRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(testPolicy));

            // Act
            CancelOrderResponse response = orderService.cancelOrder(testUser.getUserId(), testOrder.getOrderId(), request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("CANCELLED");
            assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(testBook.getStockQuantity()).isEqualTo(51); // 50 + 1 restored

            verify(bookRepository).save(testBook);
            verify(orderRepository).save(testOrder);
        }

        @Test
        @DisplayName("Should throw OrderNotCancellableException when cancelling after 48 hours")
        void shouldThrowExceptionWhenCancellingAfter48Hours() {
            // Arrange
            testOrder.setCreatedAt(Instant.now().minus(50, ChronoUnit.HOURS)); // 50 hours ago > 48 hours

            when(orderRepository.findByOrderIdAndUserUserId(testOrder.getOrderId(), testUser.getUserId()))
                    .thenReturn(Optional.of(testOrder));
            when(storePolicyRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(testPolicy));

            // Act & Assert
            assertThatThrownBy(() -> orderService.cancelOrder(
                    testUser.getUserId(), testOrder.getOrderId(), new CancelOrderRequest("Late cancel")))
                    .isInstanceOf(OrderNotCancellableException.class)
                    .hasMessageContaining("within 48 hours");
        }

        @Test
        @DisplayName("Should throw OrderNotCancellableException when order already DISPATCHED")
        void shouldThrowExceptionWhenOrderAlreadyDispatched() {
            // Arrange
            testOrder.setStatus(OrderStatus.DISPATCHED);
            testOrder.setCreatedAt(Instant.now().minus(5, ChronoUnit.HOURS));

            when(orderRepository.findByOrderIdAndUserUserId(testOrder.getOrderId(), testUser.getUserId()))
                    .thenReturn(Optional.of(testOrder));
            when(storePolicyRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(testPolicy));

            // Act & Assert
            assertThatThrownBy(() -> orderService.cancelOrder(
                    testUser.getUserId(), testOrder.getOrderId(), new CancelOrderRequest("Cancel dispatched")))
                    .isInstanceOf(OrderNotCancellableException.class)
                    .hasMessageContaining("DISPATCHED");
        }
    }
}
