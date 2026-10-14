package com.bookworm.promotion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationResponse {

    private String code;
    private boolean isValid;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal calculatedDiscount;
    private String message;
}
