package com.bookworm.promotion.service;

import com.bookworm.order.entity.Order;
import com.bookworm.promotion.dto.CouponValidateRequest;
import com.bookworm.promotion.dto.CouponValidationResponse;
import com.bookworm.promotion.entity.Coupon;
import com.bookworm.user.entity.User;

import java.math.BigDecimal;
import java.util.UUID;

public interface CouponService {

    CouponValidationResponse validateCoupon(CouponValidateRequest request, UUID userId);

    Coupon getValidCouponEntity(String code, BigDecimal cartSubtotal, UUID userId);

    void recordCouponRedemption(Coupon coupon, User user, Order order, BigDecimal discountApplied);
}
