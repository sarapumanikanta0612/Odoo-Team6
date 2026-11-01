package com.wams.qualification.dto;

import com.wams.qualification.Agreement;
import com.wams.qualification.AgreementStatus;

import java.time.Instant;

public record AgreementResponse(
        Long id,
        Long enquiryId,
        AgreementStatus status,
        String agreementReference,
        String signedByName,
        Instant signedAt,
        Long recordedByUserId,
        Instant createdAt,
        Instant updatedAt
) {
    public static AgreementResponse from(Agreement agreement) {
        return new AgreementResponse(
                agreement.getId(),
                agreement.getEnquiry().getId(),
                agreement.getStatus(),
                agreement.getAgreementReference(),
                agreement.getSignedByName(),
                agreement.getSignedAt(),
                agreement.getRecordedBy().getId(),
                agreement.getCreatedAt(),
                agreement.getUpdatedAt());
    }
}
