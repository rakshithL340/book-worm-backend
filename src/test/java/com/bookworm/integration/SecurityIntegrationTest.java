package com.bookworm.integration;

import com.bookworm.AbstractIntegrationTest;
import com.bookworm.auth.dto.AuthResponse;
import com.bookworm.auth.dto.RegisterRequest;
import com.bookworm.auth.service.AuthService;
import com.bookworm.common.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Security & RBAC Integration Tests")
class SecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtProvider jwtProvider;

    private String customerJwtToken;
    private String adminJwtToken;

    @BeforeEach
    void setUp() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest customerReq = RegisterRequest.builder()
                .email("customer_" + uniqueSuffix + "@example.com")
                .password("Password@123")
                .firstName("Customer")
                .lastName("User")
                .phone("+919876543210")
                .build();

        AuthResponse customerAuth = authService.register(customerReq);
        customerJwtToken = customerAuth.getAccessToken();

        adminJwtToken = jwtProvider.generateToken(
                UUID.randomUUID(),
                "admin_" + uniqueSuffix + "@bookworm.example.com",
                "ROLE_ADMIN"
        );
    }

    @Nested
    @DisplayName("Public Endpoints Tests")
    class PublicEndpointsTests {

        @Test
        @DisplayName("GET /categories - Should be accessible without authentication")
        void shouldAllowAccessToCategoriesWithoutAuth() throws Exception {
            mockMvc.perform(get("/categories"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /books - Should be accessible without authentication")
        void shouldAllowAccessToBooksWithoutAuth() throws Exception {
            mockMvc.perform(get("/books"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /cart - Should be accessible with guest X-Session-Token header")
        void shouldAllowGuestAccessToCartWithSessionToken() throws Exception {
            mockMvc.perform(get("/cart")
                            .header("X-Session-Token", UUID.randomUUID().toString()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Protected Customer Endpoints Tests")
    class ProtectedCustomerEndpointsTests {

        @Test
        @DisplayName("GET /users/profile - Should return 401 Unauthorized when token is missing")
        void shouldRejectUserProfileWhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/users/profile"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.title").value("Unauthorized"));
        }

        @Test
        @DisplayName("GET /users/profile - Should succeed when valid customer JWT Bearer token is provided")
        void shouldAllowUserProfileWithValidCustomerToken() throws Exception {
            mockMvc.perform(get("/users/profile")
                            .header("Authorization", "Bearer " + customerJwtToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").exists());
        }

        @Test
        @DisplayName("GET /wishlist - Should return 401 Unauthorized without token")
        void shouldRejectWishlistWithoutAuth() throws Exception {
            mockMvc.perform(get("/wishlist"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /wishlist - Should return 200 OK with customer token")
        void shouldAllowWishlistWithCustomerToken() throws Exception {
            mockMvc.perform(get("/wishlist")
                            .header("Authorization", "Bearer " + customerJwtToken))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Administrator RBAC Endpoints Tests")
    class AdminRbacEndpointsTests {

        @Test
        @DisplayName("GET /admin/dashboard - Should return 403 Forbidden when accessed with customer token")
        void shouldForbidCustomerFromAdminEndpoint() throws Exception {
            mockMvc.perform(get("/admin/dashboard")
                            .header("Authorization", "Bearer " + customerJwtToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.title").value("Forbidden"));
        }

        @Test
        @DisplayName("GET /admin/dashboard - Should allow access with administrator token")
        void shouldAllowAdminAccessWithAdminToken() throws Exception {
            // Endpoints under /admin/** require ROLE_ADMIN
            mockMvc.perform(get("/admin/dashboard")
                            .header("Authorization", "Bearer " + adminJwtToken))
                    // If endpoint is mapped it returns 200, otherwise 404 (not 403/401)
                    .andExpect(status().is(status -> org.assertj.core.api.Assertions.assertThat(status.value())
                            .isNotIn(401, 403)));
        }
    }
}
