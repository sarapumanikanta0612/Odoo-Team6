package com.wams.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Partial update. Role changes are intentionally NOT supported here;
 * use a dedicated admin endpoint if role transitions are needed later.
 */
public record UpdateUserRequest(
        @Size(max = 150) String fullName,
        @Email @Size(max = 190) String email
) {}
