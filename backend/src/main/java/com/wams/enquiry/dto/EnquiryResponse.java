package com.wams.enquiry.dto;

import com.wams.enquiry.Enquiry;
import com.wams.enquiry.EnquirySource;
import com.wams.enquiry.EnquiryStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * An enquiry as returned by the API.
 * <p>
 * The public endpoint returns this to the person who submitted the enquiry, so it must
 * only contain what they submitted plus tracking fields. Staff-only data (assigned
 * planner, internal notes, follow-ups) belongs in a separate staff response.
 */
public record EnquiryResponse(
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
		Instant createdAt,
		Instant updatedAt
) {

	public static EnquiryResponse from(Enquiry enquiry) {
		return new EnquiryResponse(
				enquiry.getId(),
				enquiry.getContactName(),
				enquiry.getEmail(),
				enquiry.getPhone(),
				enquiry.getWeddingStartDate(),
				enquiry.getWeddingEndDate(),
				enquiry.getDestination(),
				enquiry.getEstimatedGuestCount(),
				enquiry.getBudgetMin(),
				enquiry.getBudgetMax(),
				enquiry.getServicesOfInterest(),
				enquiry.getThemeStyle(),
				enquiry.getFunctionsRequired(),
				enquiry.getSpecialRequests(),
				enquiry.getMessage(),
				enquiry.getPreferredConsultationDate(),
				enquiry.getStatus(),
				enquiry.getSource(),
				enquiry.getCreatedAt(),
				enquiry.getUpdatedAt());
	}

}
