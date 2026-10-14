package com.bookworm.order.service;

import com.bookworm.common.dto.PageResponse;
import com.bookworm.order.dto.CancelOrderRequest;
import com.bookworm.order.dto.CancelOrderResponse;
import com.bookworm.order.dto.OrderCalculateRequest;
import com.bookworm.order.dto.OrderCheckoutRequest;
import com.bookworm.order.dto.OrderCheckoutResponse;
import com.bookworm.order.dto.OrderDetailResponse;
import com.bookworm.order.dto.OrderPriceCalculationResponse;
import com.bookworm.order.dto.OrderSummaryResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    OrderPriceCalculationResponse calculatePrice(UUID userId, OrderCalculateRequest request);

    OrderCheckoutResponse checkout(UUID userId, OrderCheckoutRequest request);

    PageResponse<OrderSummaryResponse> getOrders(UUID userId, Pageable pageable);

    OrderDetailResponse getOrderById(UUID userId, UUID orderId);

    CancelOrderResponse cancelOrder(UUID userId, UUID orderId, CancelOrderRequest request);

    void buyAgain(UUID userId, UUID orderId);
}
