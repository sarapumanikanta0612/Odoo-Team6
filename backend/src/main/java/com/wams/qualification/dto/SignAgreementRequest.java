package com.wams.qualification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record SignAgreementRequest(
        @NotBlank @Size(max = 100) String agreementReference,
        @NotBlank @Size(max = 150) String signedByName,
        @NotNull @PastOrPresent Instant signedAt
) {}
