package com.wams.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Explicit enquiry conversion. Primary client and lead planner are derived from
 * persisted enquiry associations; callers cannot choose unrelated accounts.
 */
public record ConvertEnquiryRequest(
        @NotBlank @Size(max = 200) String projectName,
        @Positive Long partnerClientId,
        @NotNull LocalDate weddingDate,
        @Size(max = 300) String venue
) {}
