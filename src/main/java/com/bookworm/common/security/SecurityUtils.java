package com.bookworm.common.security;

import com.bookworm.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final JwtProvider jwtProvider;

    public UUID getCurrentUserId() {
        UUID userId = getCurrentUserIdOptional();
        if (userId == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return userId;
    }

    public UUID getCurrentUserIdOptional() {
        // 1. Check Spring Security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserPrincipal principal) {
            if (principal.getUserId() != null) {
                return principal.getUserId();
            }
        }

        // 2. Check HTTP request headers
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (jwtProvider.validateToken(token)) {
                try {
                    return jwtProvider.extractUserId(token);
                } catch (Exception e) {
                    log.error("Failed to extract userId from JWT token: {}", e.getMessage());
                }
            }
        }

        // 3. Fallback check for simulated/test header
        String devUserId = request.getHeader("X-User-Id");
        if (devUserId != null && !devUserId.isBlank()) {
            try {
                return UUID.fromString(devUserId.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }

        return null;
    }
}
