package com.bookworm.order.dto;

import com.bookworm.user.dto.AddressResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {

    private UUID orderId;
    private String orderNumber;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal deliveryCharge;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private LocalDate tentativeDeliveryDate;
    private Instant createdAt;
    private boolean isCancellable;
    private AddressResponse shippingAddress;
    private List<OrderItemResponse> items;
}
