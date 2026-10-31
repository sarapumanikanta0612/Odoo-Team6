package com.wams.service;

import com.wams.dto.request.LoginRequest;
import com.wams.dto.request.RegisterRequest;
import com.wams.dto.response.AuthResponse;
import com.wams.dto.response.UserResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse register(RegisterRequest request);
    UserResponse currentUser(String email);
}
