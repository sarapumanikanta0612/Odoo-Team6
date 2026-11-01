package com.wams.dto.response;

import com.wams.entity.WeddingProject;
import com.wams.enums.ProjectStatus;
import com.wams.qualification.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
public class ProjectResponse {
    private final Long id;
    private final Long enquiryId;
    private final String projectName;
    private final Long primaryClientId;
    private final Long partnerClientId;
    private final Long leadPlannerId;
    private final LocalDate weddingDate;
    private final String venue;
    private final String selectedPackage;
    private final BigDecimal agreedAmount;
    private final BigDecimal bookingDepositAmount;
    private final String bookingDepositReference;
    private final LocalDate bookingDepositPaidOn;
    private final PaymentMethod bookingDepositMethod;
    private final ProjectStatus projectStatus;
    private final boolean agreementSigned;
    private final boolean bookingDepositPaid;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static ProjectResponse from(WeddingProject p) {
        return ProjectResponse.builder()
                .id(p.getId())
                .enquiryId(p.getEnquiry() == null ? null : p.getEnquiry().getId())
                .projectName(p.getProjectName())
                .primaryClientId(p.getPrimaryClient().getId())
                .partnerClientId(p.getPartnerClient() == null ? null : p.getPartnerClient().getId())
                .leadPlannerId(p.getLeadPlanner().getId())
                .weddingDate(p.getWeddingDate())
                .venue(p.getVenue())
                .selectedPackage(p.getSelectedPackage())
                .agreedAmount(p.getAgreedAmount())
                .bookingDepositAmount(p.getBookingDepositAmount())
                .bookingDepositReference(p.getBookingDepositReference())
                .bookingDepositPaidOn(p.getBookingDepositPaidOn())
                .bookingDepositMethod(p.getBookingDepositMethod())
                .projectStatus(p.getProjectStatus())
                .agreementSigned(p.isAgreementSigned())
                .bookingDepositPaid(p.isBookingDepositPaid())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
