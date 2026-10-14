package com.bookworm.payment.service;

import com.bookworm.common.exception.InsufficientWalletBalanceException;
import com.bookworm.order.entity.Order;
import com.bookworm.order.entity.OrderStatus;
import com.bookworm.order.repository.OrderRepository;
import com.bookworm.payment.dto.PaymentInitiateRequest;
import com.bookworm.payment.dto.PaymentInitiateResponse;
import com.bookworm.payment.dto.WalletBalanceResponse;
import com.bookworm.payment.entity.PaymentStatus;
import com.bookworm.payment.entity.PaymentTransaction;
import com.bookworm.payment.entity.WalletAccount;
import com.bookworm.payment.entity.WalletTransaction;
import com.bookworm.payment.mapper.PaymentMapper;
import com.bookworm.payment.repository.PaymentTransactionRepository;
import com.bookworm.payment.repository.WalletAccountRepository;
import com.bookworm.payment.repository.WalletTransactionRepository;
import com.bookworm.promotion.service.LoyaltyService;
import com.bookworm.user.entity.User;
import com.bookworm.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private WalletAccountRepository walletAccountRepository;
    @Mock
    private WalletTransactionRepository walletTransactionRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LoyaltyService loyaltyService;
    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User testUser;
    private Order testOrder;
    private WalletAccount testWallet;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();

        testUser = User.builder().userId(userId).email("payer@example.com").build();

        testOrder = Order.builder()
                .orderId(orderId)
                .orderNumber("ORD-999")
                .user(testUser)
                .totalAmount(new BigDecimal("500.00"))
                .status(OrderStatus.CREATED)
                .build();

        testWallet = WalletAccount.builder()
                .walletId(walletId)
                .user(testUser)
                .balance(new BigDecimal("1000.00"))
                .currencyCode("INR")
                .build();
    }

    @Test
    @DisplayName("Should successfully process wallet payment, deduct balance, and confirm order")
    void shouldProcessWalletPaymentSuccessfully() {
        // Arrange
        PaymentInitiateRequest request = PaymentInitiateRequest.builder()
                .orderId(testOrder.getOrderId())
                .paymentMethod("WALLET")
                .amount(new BigDecimal("500.00"))
                .build();

        when(orderRepository.findByOrderIdAndUserUserId(testOrder.getOrderId(), testUser.getUserId()))
                .thenReturn(Optional.of(testOrder));
        when(walletAccountRepository.findByUserUserId(testUser.getUserId())).thenReturn(Optional.of(testWallet));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> {
            PaymentTransaction pt = i.getArgument(0);
            pt.setTransactionId(UUID.randomUUID());
            return pt;
        });

        // Act
        PaymentInitiateResponse response = paymentService.processPayment(testUser.getUserId(), request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS.name());
        assertThat(testWallet.getBalance()).isEqualByComparingTo("500.00"); // 1000 - 500
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        verify(walletAccountRepository).save(testWallet);
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
        verify(orderRepository).save(testOrder);
        verify(loyaltyService).awardPointsForOrder(testUser, testOrder, new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("Should throw InsufficientWalletBalanceException when wallet balance is lower than amount")
    void shouldThrowInsufficientBalanceWhenWalletFundsAreLow() {
        // Arrange
        testWallet.setBalance(new BigDecimal("200.00")); // only 200 available, need 500

        PaymentInitiateRequest request = PaymentInitiateRequest.builder()
                .orderId(testOrder.getOrderId())
                .paymentMethod("WALLET")
                .amount(new BigDecimal("500.00"))
                .build();

        when(orderRepository.findByOrderIdAndUserUserId(testOrder.getOrderId(), testUser.getUserId()))
                .thenReturn(Optional.of(testOrder));
        when(walletAccountRepository.findByUserUserId(testUser.getUserId())).thenReturn(Optional.of(testWallet));

        // Act & Assert
        assertThatThrownBy(() -> paymentService.processPayment(testUser.getUserId(), request))
                .isInstanceOf(InsufficientWalletBalanceException.class)
                .hasMessageContaining("Insufficient wallet balance");
    }

    @Test
    @DisplayName("Should top up wallet balance successfully")
    void shouldTopUpWalletBalance() {
        // Arrange
        BigDecimal topUpAmount = new BigDecimal("250.00");
        when(walletAccountRepository.findByUserUserId(testUser.getUserId())).thenReturn(Optional.of(testWallet));
        when(walletAccountRepository.save(testWallet)).thenReturn(testWallet);
        when(paymentMapper.toWalletResponse(testWallet)).thenReturn(
                WalletBalanceResponse.builder().balance(new BigDecimal("1250.00")).currencyCode("INR").build());

        // Act
        WalletBalanceResponse response = paymentService.topUpWallet(testUser.getUserId(), topUpAmount);

        // Assert
        assertThat(response).isNotNull();
        assertThat(testWallet.getBalance()).isEqualByComparingTo("1250.00"); // 1000 + 250
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
    }
}
