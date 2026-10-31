package com.wams.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateClientRequest(
        @Size(max = 30) String phoneNumber,
        @Size(max = 500) String address
) {}
