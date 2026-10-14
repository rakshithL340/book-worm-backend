package com.bookworm.order.dto;

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
public class OrderPriceCalculationResponse {

    private Integer itemCount;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal deliveryCharge;
    private boolean isFreeDelivery;
    private String couponCodeApplied;
    private BigDecimal discountAmount;
    private BigDecimal pointsDiscount;
    private BigDecimal totalPayableAmount;
}
