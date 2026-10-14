package com.bookworm.auth.controller;

import com.bookworm.auth.dto.AuthResponse;
import com.bookworm.auth.dto.LoginRequest;
import com.bookworm.auth.dto.RegisterRequest;
import com.bookworm.auth.dto.UserSummaryDto;
import com.bookworm.auth.service.AuthService;
import com.bookworm.common.security.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController REST Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("POST /auth/register - Should return 201 Created when request is valid")
    void shouldRegisterSuccessfully() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .password("Password123!")
                .firstName("John")
                .lastName("Doe")
                .phone("+919876543210")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock-token")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(UserSummaryDto.builder()
                        .userId(UUID.randomUUID())
                        .email("newuser@example.com")
                        .firstName("John")
                        .lastName("Doe")
                        .role("ROLE_CUSTOMER")
                        .build())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("mock-token"))
                .andExpect(jsonPath("$.user.email").value("newuser@example.com"));
    }

    @Test
    @DisplayName("POST /auth/register - Should return 400 Bad Request when validation fails")
    void shouldReturnBadRequestWhenRegistrationFailsValidation() throws Exception {
        // Arrange (Invalid email and blank password)
        RegisterRequest invalidRequest = RegisterRequest.builder()
                .email("not-an-email")
                .password("")
                .firstName("")
                .lastName("")
                .build();

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("POST /auth/login - Should return 200 OK with token")
    void shouldLoginSuccessfully() throws Exception {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("existing@example.com")
                .password("Password123!")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("login-token")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(UserSummaryDto.builder().email("existing@example.com").build())
                .build();

        when(authService.login(any(LoginRequest.class), eq("session-token"))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .header("X-Session-Token", "session-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("login-token"));
    }
}
