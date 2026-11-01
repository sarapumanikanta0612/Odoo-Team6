package com.wams.qualification.dto;

import com.wams.qualification.AgreementStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateAgreementRequest(
        @NotBlank @Size(max = 100) String agreementReference,
        @NotNull AgreementStatus status,
        @Size(max = 150) String signedByName,
        @PastOrPresent Instant signedAt
) {}
