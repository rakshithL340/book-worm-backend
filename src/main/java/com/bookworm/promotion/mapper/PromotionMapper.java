package com.bookworm.promotion.mapper;

import com.bookworm.promotion.dto.CouponValidationResponse;
import com.bookworm.promotion.dto.LoyaltyPointsResponse;
import com.bookworm.promotion.entity.Coupon;
import com.bookworm.promotion.entity.LoyaltyAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PromotionMapper {

    @Mapping(target = "isValid", constant = "true")
    @Mapping(target = "discountType", expression = "java(coupon.getDiscountType() != null ? coupon.getDiscountType().name() : null)")
    @Mapping(target = "calculatedDiscount", source = "discountValue")
    @Mapping(target = "message", constant = "Coupon applied successfully")
    CouponValidationResponse toValidationResponse(Coupon coupon);

    @Mapping(target = "conversionRate", expression = "java(new java.math.BigDecimal(\"1.00\"))")
    @Mapping(target = "monetaryValue", expression = "java(account.getPointsBalance() != null ? java.math.BigDecimal.valueOf(account.getPointsBalance()).multiply(new java.math.BigDecimal(\"1.00\")) : java.math.BigDecimal.ZERO)")
    LoyaltyPointsResponse toLoyaltyResponse(LoyaltyAccount account);
}
