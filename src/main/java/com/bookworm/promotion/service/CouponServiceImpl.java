package com.bookworm.promotion.service;

import com.bookworm.common.exception.InvalidCouponException;
import com.bookworm.order.entity.Order;
import com.bookworm.promotion.dto.CouponValidateRequest;
import com.bookworm.promotion.dto.CouponValidationResponse;
import com.bookworm.promotion.entity.Coupon;
import com.bookworm.promotion.entity.CouponRedemption;
import com.bookworm.promotion.entity.DiscountType;
import com.bookworm.promotion.repository.CouponRedemptionRepository;
import com.bookworm.promotion.repository.CouponRepository;
import com.bookworm.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(CouponValidateRequest request, UUID userId) {
        log.info("Validating coupon code [{}] for cart subtotal: {}", request.getCode(), request.getCartSubtotal());

        try {
            Coupon coupon = getValidCouponEntity(request.getCode(), request.getCartSubtotal(), userId);
            BigDecimal discount = calculateDiscount(coupon, request.getCartSubtotal());

            return CouponValidationResponse.builder()
                    .code(coupon.getCode())
                    .isValid(true)
                    .discountType(coupon.getDiscountType().name())
                    .discountValue(coupon.getDiscountValue())
                    .calculatedDiscount(discount)
                    .message("Coupon applied successfully! You saved ₹" + discount)
                    .build();
        } catch (InvalidCouponException e) {
            log.warn("Coupon validation failed: {}", e.getMessage());
            return CouponValidationResponse.builder()
                    .code(request.getCode())
                    .isValid(false)
                    .discountType(null)
                    .discountValue(BigDecimal.ZERO)
                    .calculatedDiscount(BigDecimal.ZERO)
                    .message(e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Coupon getValidCouponEntity(String code, BigDecimal cartSubtotal, UUID userId) {
        if (code == null || code.trim().isEmpty()) {
            throw new InvalidCouponException("Coupon code is required");
        }

        Instant now = Instant.now();
        Coupon coupon = couponRepository.findByCodeIgnoreCaseAndActiveTrue(code.trim())
                .orElseThrow(() -> new InvalidCouponException("Invalid or inactive coupon code: " + code));

        if (coupon.getValidFrom().isAfter(now)) {
            throw new InvalidCouponException("Coupon is not active yet");
        }

        if (coupon.getValidUntil().isBefore(now)) {
            throw new InvalidCouponException("Coupon has expired");
        }

        if (coupon.getMinOrderAmount() != null && cartSubtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new InvalidCouponException("Cart subtotal of ₹" + cartSubtotal +
                    " does not meet minimum order requirement of ₹" + coupon.getMinOrderAmount());
        }

        return coupon;
    }

    @Override
    @Transactional
    public void recordCouponRedemption(Coupon coupon, User user, Order order, BigDecimal discountApplied) {
        log.info("Recording coupon redemption: couponId={}, userId={}, orderId={}, discount={}",
                coupon.getCouponId(), user.getUserId(), order.getOrderId(), discountApplied);

        CouponRedemption redemption = CouponRedemption.builder()
                .coupon(coupon)
                .user(user)
                .order(order)
                .discountApplied(discountApplied)
                .redeemedAt(Instant.now())
                .build();

        couponRedemptionRepository.save(redemption);
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal cartSubtotal) {
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal percentage = coupon.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal discount = cartSubtotal.multiply(percentage).setScale(2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
            return discount.min(cartSubtotal);
        } else {
            return coupon.getDiscountValue().min(cartSubtotal);
        }
    }
}
