package com.wams.enquiry.dto;

import com.wams.enquiry.Enquiry;
import com.wams.enquiry.EnquiryStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record InternalEnquirySummaryResponse(
        Long id,
        String contactName,
        String email,
        String phone,
        LocalDate weddingStartDate,
        LocalDate weddingEndDate,
        String destination,
        EnquiryStatus status,
        Long assignedLeadPlannerId,
        String assignedLeadPlannerName,
        Long primaryClientId,
        String primaryClientName,
        String selectedPackage,
        BigDecimal agreedAmount,
        LocalDate nextFollowUpDate,
        Instant createdAt,
        Instant updatedAt
) {
    public static InternalEnquirySummaryResponse from(Enquiry enquiry, LocalDate nextFollowUpDate) {
        var planner = enquiry.getAssignedLeadPlanner();
        return new InternalEnquirySummaryResponse(
                enquiry.getId(), enquiry.getContactName(), enquiry.getEmail(), enquiry.getPhone(),
                enquiry.getWeddingStartDate(), enquiry.getWeddingEndDate(), enquiry.getDestination(),
                enquiry.getStatus(), planner == null ? null : planner.getId(),
                planner == null ? null : planner.getUser().getFullName(),
                enquiry.getPrimaryClient() == null ? null : enquiry.getPrimaryClient().getId(),
                enquiry.getPrimaryClient() == null ? null : enquiry.getPrimaryClient().getUser().getFullName(),
                enquiry.getSelectedPackage(), enquiry.getAgreedAmount(), nextFollowUpDate,
                enquiry.getCreatedAt(), enquiry.getUpdatedAt());
    }
}
