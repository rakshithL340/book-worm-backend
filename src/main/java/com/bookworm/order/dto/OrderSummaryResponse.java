package com.bookworm.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResponse {

    private UUID orderId;
    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;
    private Integer itemCount;
    private LocalDate tentativeDeliveryDate;
    private Instant createdAt;
    private boolean isCancellable;
}
