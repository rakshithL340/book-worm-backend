package com.bookworm.order.controller;

import com.bookworm.cart.service.CartService;
import com.bookworm.common.security.JwtProvider;
import com.bookworm.common.security.SecurityUtils;
import com.bookworm.order.dto.CancelOrderRequest;
import com.bookworm.order.dto.CancelOrderResponse;
import com.bookworm.order.dto.OrderCalculateRequest;
import com.bookworm.order.dto.OrderCheckoutRequest;
import com.bookworm.order.dto.OrderCheckoutResponse;
import com.bookworm.order.dto.OrderPriceCalculationResponse;
import com.bookworm.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OrderController REST Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CartService cartService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("POST /orders/calculate - Should return calculated order breakdown")
    void shouldCalculateOrderPrice() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        OrderCalculateRequest request = OrderCalculateRequest.builder()
                .shippingAddressId(addressId)
                .build();

        OrderPriceCalculationResponse response = OrderPriceCalculationResponse.builder()
                .itemCount(2)
                .subtotal(new BigDecimal("1000.00"))
                .taxAmount(new BigDecimal("120.00"))
                .deliveryCharge(BigDecimal.ZERO)
                .isFreeDelivery(true)
                .totalPayableAmount(new BigDecimal("1120.00"))
                .build();

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(orderService.calculatePrice(eq(userId), any(OrderCalculateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/orders/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(1000.00))
                .andExpect(jsonPath("$.taxAmount").value(120.00))
                .andExpect(jsonPath("$.isFreeDelivery").value(true));
    }

    @Test
    @DisplayName("POST /orders/checkout - Should place order and return 201 Created")
    void shouldCheckoutOrder() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        OrderCheckoutRequest request = OrderCheckoutRequest.builder()
                .shippingAddressId(addressId)
                .build();

        OrderCheckoutResponse response = OrderCheckoutResponse.builder()
                .orderId(orderId)
                .orderNumber("ORD-123456")
                .status("CREATED")
                .subtotal(new BigDecimal("1000.00"))
                .totalPayableAmount(new BigDecimal("1120.00"))
                .tentativeDeliveryDate(LocalDate.now().plusDays(3))
                .build();

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(orderService.checkout(eq(userId), any(OrderCheckoutRequest.class))).thenReturn(response);

        mockMvc.perform(post("/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-123456"));
    }

    @Test
    @DisplayName("POST /orders/{orderId}/cancel - Should cancel order within 48-hour window")
    void shouldCancelOrder() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        CancelOrderRequest request = CancelOrderRequest.builder().reason("Accidental order").build();
        CancelOrderResponse response = CancelOrderResponse.builder()
                .orderId(orderId)
                .status("CANCELLED")
                .cancelledAt(Instant.now())
                .refundInitiated(true)
                .message("Order cancelled successfully. Refund initiated.")
                .build();

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(orderService.cancelOrder(eq(userId), eq(orderId), any())).thenReturn(response);

        mockMvc.perform(post("/orders/" + orderId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundInitiated").value(true));
    }
}
