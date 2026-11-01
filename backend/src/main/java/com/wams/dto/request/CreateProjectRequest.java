package com.wams.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request to convert a qualified enquiry into an official WeddingProject.
 * Agreement and deposit state are deliberately absent: the service derives
 * both from persisted records associated with {@code enquiryId}.
 *
 * Unknown legacy fields (including agreementSigned/bookingDepositPaid) are
 * ignored, but can never influence the gate.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateProjectRequest(
        @NotBlank @Size(max = 200) String projectName,
        @NotNull Long primaryClientId,
        Long partnerClientId,
        @NotNull Long leadPlannerId,
        @NotNull LocalDate weddingDate,
        @Size(max = 300) String venue,
        @NotNull Long enquiryId
) {}
