package com.bookworm.auth.service;

import com.bookworm.auth.dto.AuthResponse;
import com.bookworm.auth.dto.LoginRequest;
import com.bookworm.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request, String sessionToken);

    void logout(String token);
}
