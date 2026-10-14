package com.bookworm.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class JwtProvider {

    @Value("${application.security.jwt.secret-key:bookworm-secure-and-robust-default-jwt-secret-key-32chars}")
    private String secretKey;

    @Value("${application.security.jwt.expiration-seconds:86400}")
    private long expirationInSeconds;

    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String role = userPrincipal.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_CUSTOMER");

        return generateToken(userPrincipal.getUserId(), userPrincipal.getEmail(), role);
    }

    public String generateToken(UUID userId, String email, String role) {
        long now = System.currentTimeMillis() / 1000;
        long exp = now + expirationInSeconds;

        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payloadJson = String.format(
                "{\"sub\":\"%s\",\"email\":\"%s\",\"role\":\"%s\",\"iat\":%d,\"exp\":%d}",
                userId != null ? userId.toString() : "",
                email != null ? email : "",
                role != null ? role : "ROLE_CUSTOMER",
                now,
                exp
        );
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        String signature = sign(header + "." + payload, secretKey);
        return header + "." + payload + "." + signature;
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            String expectedSignature = sign(parts[0] + "." + parts[1], secretKey);
            if (!MessageDigest.isEqual(parts[2].getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
                log.warn("JWT signature mismatch");
                return false;
            }
            String payload = decodePayload(token);
            String expStr = extractJsonField(payload, "exp");
            if (expStr != null) {
                long exp = Long.parseLong(expStr);
                long now = System.currentTimeMillis() / 1000;
                if (now >= exp) {
                    log.warn("JWT token is expired");
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    public UUID extractUserId(String token) {
        String payload = decodePayload(token);
        String sub = extractJsonField(payload, "sub");
        return (sub != null && !sub.isBlank()) ? UUID.fromString(sub) : null;
    }

    public String extractEmail(String token) {
        String payload = decodePayload(token);
        return extractJsonField(payload, "email");
    }

    public String extractRole(String token) {
        String payload = decodePayload(token);
        return extractJsonField(payload, "role");
    }

    public Authentication getAuthentication(String token) {
        UUID userId = extractUserId(token);
        String email = extractEmail(token);
        String role = extractRole(token);

        if (role == null || role.isBlank()) {
            role = "ROLE_CUSTOMER";
        }

        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));

        UserPrincipal principal = UserPrincipal.builder()
                .userId(userId)
                .email(email)
                .password("")
                .authorities(authorities)
                .build();

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public long getExpirationInSeconds() {
        return expirationInSeconds;
    }

    private String sign(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error signing JWT", e);
        }
    }

    private String decodePayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid JWT token format");
        }
        byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private String extractJsonField(String json, String field) {
        String search = "\"" + field + "\":";
        int start = json.indexOf(search);
        if (start == -1) {
            return null;
        }
        start += search.length();
        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf('"', start);
            return json.substring(start, end);
        } else {
            int end = json.indexOf(',', start);
            if (end == -1) {
                end = json.indexOf('}', start);
            }
            return json.substring(start, end).trim();
        }
    }
}
