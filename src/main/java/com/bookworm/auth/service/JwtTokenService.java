package com.bookworm.auth.service;

import java.util.UUID;

public interface JwtTokenService {

    String generateToken(UUID userId, String email, String role);

    UUID extractUserId(String token);

    String extractEmail(String token);

    String extractRole(String token);

    boolean validateToken(String token);

    long getExpirationTimeSeconds();
}
