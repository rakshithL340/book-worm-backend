package com.bookworm.auth.service;

import com.bookworm.auth.dto.AuthResponse;
import com.bookworm.auth.dto.LoginRequest;
import com.bookworm.auth.dto.RegisterRequest;
import com.bookworm.auth.dto.UserSummaryDto;
import com.bookworm.auth.mapper.AuthMapper;
import com.bookworm.cart.service.CartService;
import com.bookworm.common.exception.ConflictException;
import com.bookworm.common.exception.UnauthorizedException;
import com.bookworm.common.security.JwtProvider;
import com.bookworm.promotion.repository.LoyaltyAccountRepository;
import com.bookworm.user.entity.User;
import com.bookworm.user.entity.UserRole;
import com.bookworm.user.repository.UserRepository;
import com.bookworm.wishlist.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthMapper authMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private LoyaltyAccountRepository loyaltyAccountRepository;

    @Mock
    private CartService cartService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private UserSummaryDto userSummaryDto;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        testUser = User.builder()
                .userId(userId)
                .email("john.doe@example.com")
                .passwordHash("hashedPassword123")
                .firstName("John")
                .lastName("Doe")
                .phone("+919876543210")
                .role(UserRole.ROLE_CUSTOMER)
                .build();

        registerRequest = RegisterRequest.builder()
                .email("john.doe@example.com")
                .password("Password@123")
                .firstName("John")
                .lastName("Doe")
                .phone("+919876543210")
                .build();

        loginRequest = LoginRequest.builder()
                .email("john.doe@example.com")
                .password("Password@123")
                .build();

        userSummaryDto = UserSummaryDto.builder()
                .userId(userId)
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .role("ROLE_CUSTOMER")
                .build();
    }

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should successfully register a new user and generate JWT token")
        void shouldRegisterNewUserSuccessfully() {
            // Arrange
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(authMapper.toEntity(registerRequest)).thenReturn(testUser);
            when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashedPassword123");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtProvider.generateToken(eq(testUser.getUserId()), eq(testUser.getEmail()), anyString()))
                    .thenReturn("mock.jwt.token");
            when(jwtProvider.getExpirationInSeconds()).thenReturn(86400L);
            when(authMapper.toUserSummaryDto(testUser)).thenReturn(userSummaryDto);

            // Act
            AuthResponse response = authService.register(registerRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("mock.jwt.token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getExpiresIn()).isEqualTo(86400L);
            assertThat(response.getUser()).isEqualTo(userSummaryDto);

            verify(wishlistRepository).save(any());
            verify(loyaltyAccountRepository).save(any());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw ConflictException when registering with duplicate email")
        void shouldThrowConflictWhenEmailAlreadyExists() {
            // Arrange
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("already exists");

            verify(userRepository, never()).save(any());
            verify(wishlistRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should successfully authenticate user and merge guest cart if session token provided")
        void shouldLoginSuccessfullyAndMergeGuestCart() {
            // Arrange
            String sessionToken = "session-uuid-123";
            when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPasswordHash())).thenReturn(true);
            when(jwtProvider.generateToken(eq(testUser.getUserId()), eq(testUser.getEmail()), anyString()))
                    .thenReturn("mock.jwt.token");
            when(jwtProvider.getExpirationInSeconds()).thenReturn(86400L);
            when(authMapper.toUserSummaryDto(testUser)).thenReturn(userSummaryDto);

            // Act
            AuthResponse response = authService.login(loginRequest, sessionToken);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("mock.jwt.token");
            assertThat(response.getUser().getEmail()).isEqualTo("john.doe@example.com");

            verify(cartService).mergeCart(testUser.getUserId(), sessionToken);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when user not found")
        void shouldThrowUnauthorizedWhenUserNotFound() {
            // Arrange
            when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest, null))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid email or password");
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when password does not match")
        void shouldThrowUnauthorizedWhenPasswordMismatch() {
            // Arrange
            when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPasswordHash())).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest, null))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid email or password");
        }
    }
}
