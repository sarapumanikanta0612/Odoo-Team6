package com.wams.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateClientRequest(
        @NotNull Long userId,
        @Size(max = 30) String phoneNumber,
        @Size(max = 500) String address
) {}
