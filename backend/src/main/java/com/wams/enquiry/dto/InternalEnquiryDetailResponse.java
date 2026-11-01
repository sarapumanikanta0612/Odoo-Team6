package com.wams.enquiry.dto;

import com.wams.enquiry.ClientLinkSource;
import com.wams.enquiry.Enquiry;
import com.wams.entity.Client;
import com.wams.enquiry.EnquirySource;
import com.wams.enquiry.EnquiryStatus;
import com.wams.qualification.AgreementStatus;
import com.wams.qualification.BookingDepositStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record InternalEnquiryDetailResponse(
        Long id,
        String contactName,
        String email,
        String phone,
        LocalDate weddingStartDate,
        LocalDate weddingEndDate,
        String destination,
        Integer estimatedGuestCount,
        BigDecimal budgetMin,
        BigDecimal budgetMax,
        String servicesOfInterest,
        String themeStyle,
        String functionsRequired,
        String specialRequests,
        String message,
        LocalDate preferredConsultationDate,
        EnquiryStatus status,
        EnquirySource source,
        Long assignedLeadPlannerId,
        String assignedLeadPlannerName,
        Long primaryClientId,
        Long primaryClientUserId,
        String primaryClientName,
        String primaryClientEmail,
        ClientLinkSource clientLinkSource,
        Instant clientLinkedAt,
        String selectedPackage,
        String packageDescription,
        BigDecimal agreedAmount,
        AgreementStatus agreementStatus,
        BookingDepositStatus bookingDepositStatus,
        Long convertedProjectId,
        List<EnquiryStatus> allowedStatusTransitions,
        Instant createdAt,
        Instant updatedAt
) {
    public static InternalEnquiryDetailResponse from(
            Enquiry enquiry,
            Client associatedClient,
            AgreementStatus agreementStatus,
            BookingDepositStatus bookingDepositStatus,
            Long projectId,
            List<EnquiryStatus> transitions) {
        var planner = enquiry.getAssignedLeadPlanner();
        return new InternalEnquiryDetailResponse(
                enquiry.getId(), enquiry.getContactName(), enquiry.getEmail(), enquiry.getPhone(),
                enquiry.getWeddingStartDate(), enquiry.getWeddingEndDate(), enquiry.getDestination(),
                enquiry.getEstimatedGuestCount(), enquiry.getBudgetMin(), enquiry.getBudgetMax(),
                enquiry.getServicesOfInterest(), enquiry.getThemeStyle(), enquiry.getFunctionsRequired(),
                enquiry.getSpecialRequests(), enquiry.getMessage(), enquiry.getPreferredConsultationDate(),
                enquiry.getStatus(), enquiry.getSource(), planner == null ? null : planner.getId(),
                planner == null ? null : planner.getUser().getFullName(),
                associatedClient == null ? null : associatedClient.getId(),
                associatedClient == null ? null : associatedClient.getUser().getId(),
                associatedClient == null ? null : associatedClient.getUser().getFullName(),
                associatedClient == null ? null : associatedClient.getUser().getEmail(),
                enquiry.getClientLinkSource(), enquiry.getClientLinkedAt(),
                enquiry.getSelectedPackage(), enquiry.getPackageDescription(), enquiry.getAgreedAmount(),
                agreementStatus, bookingDepositStatus, projectId, transitions,
                enquiry.getCreatedAt(), enquiry.getUpdatedAt());
    }
}
