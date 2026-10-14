package com.bookworm.cart.controller;

import com.bookworm.cart.dto.AddToCartRequest;
import com.bookworm.cart.dto.CartItemResponse;
import com.bookworm.cart.dto.CartResponse;
import com.bookworm.cart.dto.UpdateQuantityRequest;
import com.bookworm.cart.service.CartService;
import com.bookworm.common.security.JwtProvider;
import com.bookworm.common.security.SecurityUtils;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CartController REST Tests")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("GET /cart - Should return active cart for guest with X-Session-Token")
    void shouldReturnActiveCartForGuest() throws Exception {
        UUID cartId = UUID.randomUUID();
        CartResponse cartResponse = CartResponse.builder()
                .cartId(cartId)
                .totalItems(1)
                .subtotal(new BigDecimal("599.00"))
                .items(List.of(CartItemResponse.builder().title("Atomic Habits").quantity(1).build()))
                .build();

        when(securityUtils.getCurrentUserIdOptional()).thenReturn(null);
        when(cartService.getCart(null, "guest-session-123")).thenReturn(cartResponse);

        mockMvc.perform(get("/cart")
                        .header("X-Session-Token", "guest-session-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(cartId.toString()))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    @DisplayName("POST /cart/items - Should add item to cart")
    void shouldAddItemToCart() throws Exception {
        UUID bookId = UUID.randomUUID();
        AddToCartRequest request = AddToCartRequest.builder()
                .bookId(bookId)
                .quantity(2)
                .build();

        CartResponse cartResponse = CartResponse.builder()
                .totalItems(2)
                .subtotal(new BigDecimal("1198.00"))
                .build();

        when(securityUtils.getCurrentUserIdOptional()).thenReturn(null);
        when(cartService.addToCart(eq(null), eq("guest-session-123"), any(AddToCartRequest.class)))
                .thenReturn(cartResponse);

        mockMvc.perform(post("/cart/items")
                        .header("X-Session-Token", "guest-session-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    @DisplayName("PUT /cart/items/{id} - Should update item quantity with stepper")
    void shouldUpdateCartItemQuantity() throws Exception {
        UUID cartItemId = UUID.randomUUID();
        UpdateQuantityRequest request = UpdateQuantityRequest.builder().quantity(3).build();

        CartResponse cartResponse = CartResponse.builder()
                .totalItems(3)
                .subtotal(new BigDecimal("1797.00"))
                .build();

        when(securityUtils.getCurrentUserIdOptional()).thenReturn(null);
        when(cartService.updateQuantity(eq(null), eq("guest-session-123"), eq(cartItemId), any(UpdateQuantityRequest.class)))
                .thenReturn(cartResponse);

        mockMvc.perform(put("/cart/items/" + cartItemId)
                        .header("X-Session-Token", "guest-session-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3));
    }

    @Test
    @DisplayName("DELETE /cart/items/{id} - Should remove item from cart")
    void shouldRemoveCartItem() throws Exception {
        UUID cartItemId = UUID.randomUUID();
        CartResponse cartResponse = CartResponse.builder().totalItems(0).subtotal(BigDecimal.ZERO).build();

        when(securityUtils.getCurrentUserIdOptional()).thenReturn(null);
        when(cartService.removeFromCart(null, "guest-session-123", cartItemId)).thenReturn(cartResponse);

        mockMvc.perform(delete("/cart/items/" + cartItemId)
                        .header("X-Session-Token", "guest-session-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }
}
