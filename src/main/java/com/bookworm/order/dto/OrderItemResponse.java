package com.bookworm.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    private UUID bookId;
    private String bookTitle;
    private String bookFormat;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
