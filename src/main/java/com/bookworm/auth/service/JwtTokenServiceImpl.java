package com.bookworm.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
public class JwtTokenServiceImpl implements JwtTokenService {

    @Value("${application.security.jwt.secret-key:bookworm-secure-and-robust-default-jwt-secret-key-32chars}")
    private String secretKey;

    @Value("${application.security.jwt.expiration-seconds:86400}")
    private long expirationSeconds;

    @Override
    public String generateToken(UUID userId, String email, String role) {
        long now = System.currentTimeMillis() / 1000;
        long exp = now + expirationSeconds;

        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payloadJson = String.format(
                "{\"sub\":\"%s\",\"email\":\"%s\",\"role\":\"%s\",\"iat\":%d,\"exp\":%d}",
                userId.toString(), email, role, now, exp
        );
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        String signature = sign(header + "." + payload, secretKey);
        return header + "." + payload + "." + signature;
    }

    @Override
    public UUID extractUserId(String token) {
        String payload = decodePayload(token);
        String sub = extractJsonField(payload, "sub");
        return UUID.fromString(sub);
    }

    @Override
    public String extractEmail(String token) {
        String payload = decodePayload(token);
        return extractJsonField(payload, "email");
    }

    @Override
    public String extractRole(String token) {
        String payload = decodePayload(token);
        return extractJsonField(payload, "role");
    }

    @Override
    public boolean validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            String expectedSignature = sign(parts[0] + "." + parts[1], secretKey);
            if (!MessageDigest.isEqual(parts[2].getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
                return false;
            }
            String payload = decodePayload(token);
            String expStr = extractJsonField(payload, "exp");
            if (expStr != null) {
                long exp = Long.parseLong(expStr);
                long now = System.currentTimeMillis() / 1000;
                return now < exp;
            }
            return true;
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public long getExpirationTimeSeconds() {
        return expirationSeconds;
    }

    private String sign(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error calculating HMAC-SHA256", e);
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
