package com.wams.dto.response;

import com.wams.entity.User;
import com.wams.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/** Never exposes passwordHash. */
@Getter
@Builder
public class UserResponse {
    private final Long id;
    private final String fullName;
    private final String email;
    private final Role role;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static UserResponse from(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .role(u.getRole())
                .active(u.isActive())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}
