package com.bookworm.promotion.service;

import com.bookworm.common.exception.BadRequestException;
import com.bookworm.common.exception.ResourceNotFoundException;
import com.bookworm.order.entity.Order;
import com.bookworm.promotion.dto.LoyaltyPointsResponse;
import com.bookworm.promotion.entity.LoyaltyAccount;
import com.bookworm.promotion.entity.LoyaltyTransaction;
import com.bookworm.promotion.mapper.PromotionMapper;
import com.bookworm.promotion.repository.LoyaltyAccountRepository;
import com.bookworm.promotion.repository.LoyaltyTransactionRepository;
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
public class LoyaltyServiceImpl implements LoyaltyService {

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final UserRepository userRepository;
    private final PromotionMapper promotionMapper;

    @Override
    @Transactional
    public LoyaltyPointsResponse getLoyaltyPoints(UUID userId) {
        log.info("Fetching loyalty points balance for user: {}", userId);
        LoyaltyAccount account = getOrCreateLoyaltyAccount(userId);
        return promotionMapper.toLoyaltyResponse(account);
    }

    @Override
    @Transactional
    public void redeemPoints(User user, Order order, int pointsToRedeem) {
        if (pointsToRedeem <= 0) {
            return;
        }

        log.info("Redeeming {} loyalty points for user {} on order {}",
                pointsToRedeem, user.getUserId(), order != null ? order.getOrderId() : null);

        LoyaltyAccount account = getOrCreateLoyaltyAccount(user.getUserId());
        if (account.getPointsBalance() < pointsToRedeem) {
            throw new BadRequestException("Insufficient loyalty points. Available: " +
                    account.getPointsBalance() + ", Requested: " + pointsToRedeem);
        }

        int newBalance = account.getPointsBalance() - pointsToRedeem;
        account.setPointsBalance(newBalance);
        loyaltyAccountRepository.save(account);

        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .loyaltyAccount(account)
                .order(order)
                .transactionType("REDEEMED")
                .points(-pointsToRedeem)
                .balanceAfter(newBalance)
                .reason("Redeemed against Order #" + (order != null ? order.getOrderNumber() : ""))
                .createdAt(Instant.now())
                .build();
        loyaltyTransactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public void awardPointsForOrder(User user, Order order, BigDecimal paidAmount) {
        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        // Policy: 1 loyalty point for every ₹100 spent
        int pointsToAward = paidAmount.divideToIntegralValue(BigDecimal.valueOf(100)).intValue();
        if (pointsToAward <= 0) {
            return;
        }

        log.info("Awarding {} loyalty points to user {} for order {}", pointsToAward, user.getUserId(), order.getOrderId());

        LoyaltyAccount account = getOrCreateLoyaltyAccount(user.getUserId());
        int newBalance = account.getPointsBalance() + pointsToAward;
        account.setPointsBalance(newBalance);
        loyaltyAccountRepository.save(account);

        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .loyaltyAccount(account)
                .order(order)
                .transactionType("EARNED")
                .points(pointsToAward)
                .balanceAfter(newBalance)
                .reason("Earned for Order #" + order.getOrderNumber())
                .createdAt(Instant.now())
                .build();
        loyaltyTransactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public void refundPointsForOrder(User user, Order order, int pointsToRefund) {
        if (pointsToRefund <= 0) {
            return;
        }

        log.info("Restoring {} redeemed loyalty points for cancelled order {}", pointsToRefund, order.getOrderId());

        LoyaltyAccount account = getOrCreateLoyaltyAccount(user.getUserId());
        int newBalance = account.getPointsBalance() + pointsToRefund;
        account.setPointsBalance(newBalance);
        loyaltyAccountRepository.save(account);

        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .loyaltyAccount(account)
                .order(order)
                .transactionType("REFUNDED")
                .points(pointsToRefund)
                .balanceAfter(newBalance)
                .reason("Refunded for cancelled Order #" + order.getOrderNumber())
                .createdAt(Instant.now())
                .build();
        loyaltyTransactionRepository.save(transaction);
    }

    private LoyaltyAccount getOrCreateLoyaltyAccount(UUID userId) {
        return loyaltyAccountRepository.findByUserUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
                    LoyaltyAccount newAccount = LoyaltyAccount.builder()
                            .user(user)
                            .pointsBalance(0)
                            .build();
                    return loyaltyAccountRepository.save(newAccount);
                });
    }
}
