package com.wams.dto.response;

import com.wams.entity.Client;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ClientResponse {
    private final Long id;
    private final UserResponse user;
    private final String phoneNumber;
    private final String address;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static ClientResponse from(Client c) {
        return ClientResponse.builder()
                .id(c.getId())
                .user(UserResponse.from(c.getUser()))
                .phoneNumber(c.getPhoneNumber())
                .address(c.getAddress())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
