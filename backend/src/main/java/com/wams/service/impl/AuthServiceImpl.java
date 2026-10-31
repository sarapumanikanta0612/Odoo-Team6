package com.wams.service.impl;

import com.wams.dto.request.LoginRequest;
import com.wams.dto.request.RegisterRequest;
import com.wams.dto.response.AuthResponse;
import com.wams.dto.response.UserResponse;
import com.wams.entity.User;
import com.wams.exception.InvalidCredentialsException;
import com.wams.exception.ResourceNotFoundException;
import com.wams.repository.UserRepository;
import com.wams.security.JwtService;
import com.wams.service.AuthService;
import com.wams.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           UserService userService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        User u = userRepository.findByEmailIgnoreCase(r.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));
        if (!u.isActive()) {
            throw new InvalidCredentialsException("Account disabled");
        }
        if (!passwordEncoder.matches(r.password(), u.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
        return buildAuthResponse(u);
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest r) {
        UserResponse created = userService.register(r);
        User u = userRepository.findById(created.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found after register"));
        return buildAuthResponse(u);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse currentUser(String email) {
        User u = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return UserResponse.from(u);
    }

    private AuthResponse buildAuthResponse(User u) {
        return AuthResponse.builder()
                .token(jwtService.generateToken(u))
                .tokenType("Bearer")
                .expiresInSeconds(jwtService.getExpirationSeconds())
                .user(UserResponse.from(u))
                .build();
    }
}
