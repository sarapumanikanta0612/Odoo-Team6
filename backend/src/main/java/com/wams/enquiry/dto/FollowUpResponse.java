package com.wams.enquiry.dto;

import com.wams.enquiry.EnquiryFollowUp;

import java.time.Instant;
import java.time.LocalDate;

public record FollowUpResponse(
        Long id,
        Long enquiryId,
        Instant occurredAt,
        String notes,
        LocalDate nextFollowUpDate,
        Long recordedByUserId,
        String recordedByName,
        Instant createdAt
) {
    public static FollowUpResponse from(EnquiryFollowUp followUp) {
        return new FollowUpResponse(
                followUp.getId(), followUp.getEnquiry().getId(), followUp.getOccurredAt(),
                followUp.getNotes(), followUp.getNextFollowUpDate(), followUp.getRecordedBy().getId(),
                followUp.getRecordedBy().getFullName(), followUp.getCreatedAt());
    }
}
