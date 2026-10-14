package com.bookworm.promotion.service;

import com.bookworm.order.entity.Order;
import com.bookworm.promotion.dto.LoyaltyPointsResponse;
import com.bookworm.user.entity.User;

import java.math.BigDecimal;
import java.util.UUID;

public interface LoyaltyService {

    LoyaltyPointsResponse getLoyaltyPoints(UUID userId);

    void redeemPoints(User user, Order order, int pointsToRedeem);

    void awardPointsForOrder(User user, Order order, BigDecimal paidAmount);

    void refundPointsForOrder(User user, Order order, int pointsToRefund);
}
