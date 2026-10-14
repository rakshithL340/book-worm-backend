package com.bookworm.promotion.service;

import com.bookworm.common.exception.InvalidCouponException;
import com.bookworm.promotion.dto.CouponValidateRequest;
import com.bookworm.promotion.dto.CouponValidationResponse;
import com.bookworm.promotion.entity.Coupon;
import com.bookworm.promotion.entity.DiscountType;
import com.bookworm.promotion.repository.CouponRedemptionRepository;
import com.bookworm.promotion.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponService Unit Tests")
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponRedemptionRepository couponRedemptionRepository;

    @InjectMocks
    private CouponServiceImpl couponService;

    private Coupon percentageCoupon;
    private Coupon flatCoupon;

    @BeforeEach
    void setUp() {
        percentageCoupon = Coupon.builder()
                .couponId(UUID.randomUUID())
                .code("BOOK10")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00")) // 10%
                .maxDiscountAmount(new BigDecimal("100.00")) // max ₹100
                .minOrderAmount(new BigDecimal("300.00"))
                .validFrom(Instant.now().minus(1, ChronoUnit.DAYS))
                .validUntil(Instant.now().plus(30, ChronoUnit.DAYS))
                .active(true)
                .build();

        flatCoupon = Coupon.builder()
                .couponId(UUID.randomUUID())
                .code("FLAT50")
                .discountType(DiscountType.FLAT)
                .discountValue(new BigDecimal("50.00"))
                .minOrderAmount(new BigDecimal("200.00"))
                .validFrom(Instant.now().minus(1, ChronoUnit.DAYS))
                .validUntil(Instant.now().plus(30, ChronoUnit.DAYS))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should validate percentage coupon and cap at max discount")
    void shouldValidatePercentageCouponWithMaxCap() {
        // Arrange (Subtotal 1500 -> 10% is 150, but max cap is 100)
        CouponValidateRequest request = CouponValidateRequest.builder()
                .code("BOOK10")
                .cartSubtotal(new BigDecimal("1500.00"))
                .build();

        when(couponRepository.findByCodeIgnoreCaseAndActiveTrue("BOOK10"))
                .thenReturn(Optional.of(percentageCoupon));

        // Act
        CouponValidationResponse response = couponService.validateCoupon(request, UUID.randomUUID());

        // Assert
        assertThat(response.isValid()).isTrue();
        assertThat(response.getCalculatedDiscount()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Should validate flat coupon successfully")
    void shouldValidateFlatCouponSuccessfully() {
        // Arrange
        CouponValidateRequest request = CouponValidateRequest.builder()
                .code("FLAT50")
                .cartSubtotal(new BigDecimal("500.00"))
                .build();

        when(couponRepository.findByCodeIgnoreCaseAndActiveTrue("FLAT50"))
                .thenReturn(Optional.of(flatCoupon));

        // Act
        CouponValidationResponse response = couponService.validateCoupon(request, UUID.randomUUID());

        // Assert
        assertThat(response.isValid()).isTrue();
        assertThat(response.getCalculatedDiscount()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Should fail coupon validation when cart subtotal is below minimum order requirement")
    void shouldFailWhenCartSubtotalIsBelowMinOrder() {
        // Arrange (Cart subtotal 100 < Min 300)
        CouponValidateRequest request = CouponValidateRequest.builder()
                .code("BOOK10")
                .cartSubtotal(new BigDecimal("100.00"))
                .build();

        when(couponRepository.findByCodeIgnoreCaseAndActiveTrue("BOOK10"))
                .thenReturn(Optional.of(percentageCoupon));

        // Act
        CouponValidationResponse response = couponService.validateCoupon(request, UUID.randomUUID());

        // Assert
        assertThat(response.isValid()).isFalse();
        assertThat(response.getMessage()).contains("does not meet minimum order requirement");
    }

    @Test
    @DisplayName("Should throw InvalidCouponException when coupon has expired")
    void shouldThrowWhenCouponIsExpired() {
        // Arrange
        percentageCoupon.setValidUntil(Instant.now().minus(2, ChronoUnit.DAYS));
        when(couponRepository.findByCodeIgnoreCaseAndActiveTrue("BOOK10"))
                .thenReturn(Optional.of(percentageCoupon));

        // Act & Assert
        assertThatThrownBy(() -> couponService.getValidCouponEntity("BOOK10", new BigDecimal("500.00"), UUID.randomUUID()))
                .isInstanceOf(InvalidCouponException.class)
                .hasMessageContaining("expired");
    }
}
