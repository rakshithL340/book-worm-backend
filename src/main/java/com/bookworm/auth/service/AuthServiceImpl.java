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
import com.bookworm.promotion.entity.LoyaltyAccount;
import com.bookworm.promotion.repository.LoyaltyAccountRepository;
import com.bookworm.user.entity.User;
import com.bookworm.user.entity.UserRole;
import com.bookworm.user.repository.UserRepository;
import com.bookworm.wishlist.entity.Wishlist;
import com.bookworm.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final WishlistRepository wishlistRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final CartService cartService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Processing customer registration for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - email already in use: {}", request.getEmail());
            throw new ConflictException("User with email " + request.getEmail() + " already exists");
        }

        User user = authMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.ROLE_CUSTOMER);

        User savedUser = userRepository.save(user);
        log.info("Customer registered successfully with userId: {}", savedUser.getUserId());

        // Initialize empty Wishlist for new member
        Wishlist wishlist = Wishlist.builder()
                .user(savedUser)
                .build();
        wishlistRepository.save(wishlist);

        // Initialize Loyalty points account for new member
        LoyaltyAccount loyaltyAccount = LoyaltyAccount.builder()
                .user(savedUser)
                .pointsBalance(0)
                .build();
        loyaltyAccountRepository.save(loyaltyAccount);

        String token = jwtProvider.generateToken(
                savedUser.getUserId(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );

        UserSummaryDto userSummary = authMapper.toUserSummaryDto(savedUser);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtProvider.getExpirationInSeconds())
                .user(userSummary)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String sessionToken) {
        log.info("Processing user login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found with email: {}", request.getEmail());
                    return new UnauthorizedException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed - invalid password credentials for email: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }

        log.info("User authenticated successfully with userId: {}", user.getUserId());

        // Merge guest cart into user's cart if guest session token was provided
        if (sessionToken != null && !sessionToken.trim().isEmpty()) {
            log.info("Merging guest session cart [{}] into user cart [{}]", sessionToken, user.getUserId());
            try {
                cartService.mergeCart(user.getUserId(), sessionToken);
            } catch (Exception e) {
                log.error("Failed to merge guest cart during login for user: {}", user.getUserId(), e);
            }
        }

        String token = jwtProvider.generateToken(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name()
        );

        UserSummaryDto userSummary = authMapper.toUserSummaryDto(user);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtProvider.getExpirationInSeconds())
                .user(userSummary)
                .build();
    }

    @Override
    public void logout(String token) {
        log.info("Processing logout request for token");
    }
}
