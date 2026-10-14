package com.bookworm.payment.service;

import com.bookworm.common.exception.BadRequestException;
import com.bookworm.common.exception.InsufficientWalletBalanceException;
import com.bookworm.common.exception.PaymentProcessingException;
import com.bookworm.common.exception.ResourceNotFoundException;
import com.bookworm.order.entity.Order;
import com.bookworm.order.entity.OrderStatus;
import com.bookworm.order.entity.OrderStatusHistory;
import com.bookworm.order.repository.OrderRepository;
import com.bookworm.payment.dto.PaymentInitiateRequest;
import com.bookworm.payment.dto.PaymentInitiateResponse;
import com.bookworm.payment.dto.WalletBalanceResponse;
import com.bookworm.payment.entity.PaymentMethod;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final LoyaltyService loyaltyService;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaymentInitiateResponse processPayment(UUID userId, PaymentInitiateRequest request) {
        log.info("Processing payment for user: {}, order: {}, method: {}, amount: {}",
                userId, request.getOrderId(), request.getPaymentMethod(), request.getAmount());

        Order order = orderRepository.findByOrderIdAndUserUserId(request.getOrderId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", request.getOrderId()));

        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Order is not in payable status. Current status: " + order.getStatus().name());
        }

        if (request.getAmount().compareTo(order.getTotalAmount()) != 0) {
            throw new BadRequestException("Payment amount ₹" + request.getAmount() +
                    " does not match order total payable amount ₹" + order.getTotalAmount());
        }

        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unsupported payment method: " + request.getPaymentMethod());
        }

        String maskedCard = null;
        String gatewayRefId = "GW-" + System.currentTimeMillis();

        if (paymentMethod == PaymentMethod.WALLET) {
            WalletAccount wallet = getOrCreateWallet(userId);
            if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
                log.warn("Insufficient wallet balance for user {}. Balance: {}, Required: {}",
                        userId, wallet.getBalance(), request.getAmount());
                throw new InsufficientWalletBalanceException(
                        "Insufficient wallet balance. Available: ₹" + wallet.getBalance() + ", Required: ₹" + request.getAmount());
            }

            BigDecimal newBalance = wallet.getBalance().subtract(request.getAmount());
            wallet.setBalance(newBalance);
            walletAccountRepository.save(wallet);

            WalletTransaction walletTx = WalletTransaction.builder()
                    .walletAccount(wallet)
                    .order(order)
                    .transactionType("DEBIT")
                    .amount(request.getAmount())
                    .balanceAfter(newBalance)
                    .createdAt(Instant.now())
                    .build();
            walletTransactionRepository.save(walletTx);

            gatewayRefId = "WAL-" + walletTx.getTransactionId().toString().substring(0, 8);
        } else if (paymentMethod == PaymentMethod.CREDIT_CARD || paymentMethod == PaymentMethod.DEBIT_CARD) {
            if (request.getCardDetails() != null && request.getCardDetails().getCardNumber() != null) {
                String cardNum = request.getCardDetails().getCardNumber().replaceAll("\\s+", "");
                if (cardNum.length() >= 4) {
                    maskedCard = "**** **** **** " + cardNum.substring(cardNum.length() - 4);
                }
            }
        }

        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .paymentMethod(paymentMethod)
                .amount(request.getAmount())
                .currencyCode("INR")
                .status(PaymentStatus.SUCCESS)
                .gatewayRefId(gatewayRefId)
                .maskedCardNumber(maskedCard)
                .build();

        PaymentTransaction savedTx = paymentTransactionRepository.save(transaction);

        // Update Order Status to CONFIRMED
        order.setStatus(OrderStatus.CONFIRMED);
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.CONFIRMED)
                .notes("Payment completed successfully via " + paymentMethod.name() + " (Ref: " + gatewayRefId + ")")
                .build();
        order.addStatusHistory(history);
        orderRepository.save(order);

        // Award loyalty points for confirmed paid order
        loyaltyService.awardPointsForOrder(order.getUser(), order, request.getAmount());

        log.info("Payment processed successfully for order: {}. Transaction ID: {}",
                order.getOrderId(), savedTx.getTransactionId());

        return PaymentInitiateResponse.builder()
                .transactionId(savedTx.getTransactionId())
                .orderId(order.getOrderId())
                .status(PaymentStatus.SUCCESS.name())
                .gatewayRefId(gatewayRefId)
                .message("Payment completed successfully")
                .build();
    }

    @Override
    @Transactional
    public WalletBalanceResponse getWalletBalance(UUID userId) {
        log.info("Fetching wallet balance for user: {}", userId);
        WalletAccount wallet = getOrCreateWallet(userId);
        return paymentMapper.toWalletResponse(wallet);
    }

    @Override
    @Transactional
    public WalletBalanceResponse topUpWallet(UUID userId, BigDecimal amount) {
        log.info("Topping up wallet for user: {} with amount: {}", userId, amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Top-up amount must be positive");
        }

        WalletAccount wallet = getOrCreateWallet(userId);
        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        WalletAccount savedWallet = walletAccountRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .walletAccount(savedWallet)
                .transactionType("CREDIT")
                .amount(amount)
                .balanceAfter(newBalance)
                .createdAt(Instant.now())
                .build();
        walletTransactionRepository.save(transaction);

        log.info("Wallet top-up successful for user: {}. New balance: {}", userId, newBalance);
        return paymentMapper.toWalletResponse(savedWallet);
    }

    private WalletAccount getOrCreateWallet(UUID userId) {
        return walletAccountRepository.findByUserUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
                    WalletAccount newWallet = WalletAccount.builder()
                            .user(user)
                            .balance(BigDecimal.ZERO)
                            .currencyCode("INR")
                            .build();
                    return walletAccountRepository.save(newWallet);
                });
    }
}
