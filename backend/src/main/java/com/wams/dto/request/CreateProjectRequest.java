package com.wams.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request to create a WeddingProject.
 *
 * The two booleans {@code agreementSigned} and {@code bookingDepositPaid}
 * MUST both be true for the service layer to accept this request. These
 * values are expected to be supplied by trusted upstream modules (agreement
 * and deposit) once they exist, not forged by an untrusted client.
 */
public record CreateProjectRequest(
        @NotBlank @Size(max = 200) String projectName,
        @NotNull Long primaryClientId,
        Long partnerClientId,
        @NotNull Long leadPlannerId,
        @NotNull LocalDate weddingDate,
        @Size(max = 300) String venue,
        @NotNull Boolean agreementSigned,
        @NotNull Boolean bookingDepositPaid
) {}
