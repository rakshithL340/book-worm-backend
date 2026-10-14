package com.bookworm.integration;

import com.bookworm.AbstractIntegrationTest;
import com.bookworm.auth.dto.AuthResponse;
import com.bookworm.auth.dto.RegisterRequest;
import com.bookworm.auth.service.AuthService;
import com.bookworm.cart.dto.AddToCartRequest;
import com.bookworm.cart.dto.UpdateQuantityRequest;
import com.bookworm.catalog.entity.Book;
import com.bookworm.catalog.repository.BookRepository;
import com.bookworm.order.dto.CancelOrderRequest;
import com.bookworm.order.dto.OrderCalculateRequest;
import com.bookworm.order.dto.OrderCheckoutRequest;
import com.bookworm.payment.dto.PaymentInitiateRequest;
import com.bookworm.payment.service.PaymentService;
import com.bookworm.user.dto.AddressCreateRequest;
import com.bookworm.user.dto.AddressResponse;
import com.bookworm.user.service.UserService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("End-to-End Order Workflow Integration Tests")
class OrderWorkflowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookRepository bookRepository;

    private String jwtToken;
    private UUID userId;
    private UUID addressId;
    private Book catalogBook;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("shopper_" + unique + "@example.com")
                .password("StrongPass123!")
                .firstName("Shopper")
                .lastName("Worm")
                .phone("+919876543210")
                .build();

        AuthResponse authResponse = authService.register(registerRequest);
        jwtToken = authResponse.getAccessToken();
        userId = authResponse.getUser().getUserId();

        AddressCreateRequest addressReq = AddressCreateRequest.builder()
                .firstName("Shopper")
                .lastName("Worm")
                .addressLine1("123 MG Road, Brigade Towers")
                .city("Bengaluru")
                .state("Karnataka")
                .pinCode("560001")
                .phoneNumber("+919876543210")
                .isDefault(true)
                .build();

        AddressResponse addressResp = userService.addAddress(userId, addressReq);
        addressId = addressResp.getAddressId();

        // Retrieve a catalog book from seeded database or create one
        catalogBook = bookRepository.findAll().stream().findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Complete E-Commerce Order Lifecycle: Browse -> Cart -> Calculate -> Checkout -> Pay -> Cancel within 48h")
    void shouldExecuteFullOrderLifecycleSuccessfully() throws Exception {
        int initialStock = catalogBook.getStockQuantity();

        // ---------------------------------------------------------------------
        // Step 1: Browse Catalog & Check Book Availability
        // ---------------------------------------------------------------------
        mockMvc.perform(get("/books/" + catalogBook.getBookId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(catalogBook.getTitle()))
                .andExpect(jsonPath("$.price").exists());

        // ---------------------------------------------------------------------
        // Step 2: Add Book to Cart
        // ---------------------------------------------------------------------
        AddToCartRequest addToCartReq = AddToCartRequest.builder()
                .bookId(catalogBook.getBookId())
                .quantity(2)
                .build();

        MvcResult cartResult = mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCartReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andReturn();

        String cartItemId = JsonPath.read(cartResult.getResponse().getContentAsString(), "$.items[0].cartItemId");

        // ---------------------------------------------------------------------
        // Step 3: Update Quantity via Stepper
        // ---------------------------------------------------------------------
        UpdateQuantityRequest updateQtyReq = UpdateQuantityRequest.builder().quantity(1).build();

        mockMvc.perform(put("/cart/items/" + cartItemId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateQtyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1));

        // ---------------------------------------------------------------------
        // Step 4: Pre-Checkout Pricing Calculation
        // ---------------------------------------------------------------------
        OrderCalculateRequest calcReq = OrderCalculateRequest.builder()
                .shippingAddressId(addressId)
                .redeemGiftPoints(0)
                .build();

        mockMvc.perform(post("/orders/calculate")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(calcReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").exists())
                .andExpect(jsonPath("$.taxAmount").exists())
                .andExpect(jsonPath("$.totalPayableAmount").exists());

        // ---------------------------------------------------------------------
        // Step 5: Checkout / Place Order
        // ---------------------------------------------------------------------
        OrderCheckoutRequest checkoutReq = OrderCheckoutRequest.builder()
                .shippingAddressId(addressId)
                .build();

        MvcResult checkoutResult = mockMvc.perform(post("/orders/checkout")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn();

        String orderIdStr = JsonPath.read(checkoutResult.getResponse().getContentAsString(), "$.orderId");
        UUID orderId = UUID.fromString(orderIdStr);
        Double payableAmountDouble = JsonPath.read(checkoutResult.getResponse().getContentAsString(), "$.totalPayableAmount");
        BigDecimal totalPayable = BigDecimal.valueOf(payableAmountDouble);

        // Verify inventory stock was decremented in database
        Book bookAfterCheckout = bookRepository.findById(catalogBook.getBookId()).orElseThrow();
        assertThat(bookAfterCheckout.getStockQuantity()).isEqualTo(initialStock - 1);

        // ---------------------------------------------------------------------
        // Step 6: Process Payment via Digital Wallet
        // ---------------------------------------------------------------------
        // Top-up wallet first
        paymentService.topUpWallet(userId, totalPayable.add(new BigDecimal("100.00")));

        PaymentInitiateRequest paymentReq = PaymentInitiateRequest.builder()
                .orderId(orderId)
                .paymentMethod("WALLET")
                .amount(totalPayable)
                .build();

        mockMvc.perform(post("/payments/initiate")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Verify Order status is now CONFIRMED
        mockMvc.perform(get("/orders/" + orderId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.isCancellable").value(true));

        // ---------------------------------------------------------------------
        // Step 7: Cancel Order within 48-Hour Window
        // ---------------------------------------------------------------------
        CancelOrderRequest cancelReq = CancelOrderRequest.builder()
                .reason("Need to change delivery location")
                .build();

        mockMvc.perform(post("/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundInitiated").value(true));

        // Verify Order status in DB is CANCELLED
        mockMvc.perform(get("/orders/" + orderId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.isCancellable").value(false));

        // Verify Inventory Stock is completely restored in database
        Book bookAfterCancel = bookRepository.findById(catalogBook.getBookId()).orElseThrow();
        assertThat(bookAfterCancel.getStockQuantity()).isEqualTo(initialStock);
    }
}
