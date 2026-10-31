package com.wams.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private final String token;
    private final String tokenType;
    private final long expiresInSeconds;
    private final UserResponse user;
}
