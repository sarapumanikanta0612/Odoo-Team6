package com.wams.dto.request;

import com.wams.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin-only user creation. */
public record CreateUserRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role,
        Boolean active
) {}
