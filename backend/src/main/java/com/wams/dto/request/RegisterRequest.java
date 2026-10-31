package com.wams.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Public self-registration. Always creates a PRIMARY_CLIENT user.
 * Privileged roles (ADMIN, LEAD_PLANNER, EMPLOYEE, VENDOR) must be created
 * through the admin-only {@code POST /api/users} endpoint.
 */
public record RegisterRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Size(min = 8, max = 72, message = "password must be 8-72 characters") String password
) {}
