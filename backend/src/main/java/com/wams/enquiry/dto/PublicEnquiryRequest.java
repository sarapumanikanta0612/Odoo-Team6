package com.wams.enquiry.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Body of {@code POST /api/public/enquiries}, submitted by the public website form.
 * <p>
 * There is deliberately no status, source or id field. The server always records a
 * public enquiry as NEW from WEBSITE, and unknown JSON properties such as "status"
 * are ignored. Amounts are in INR.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PublicEnquiryRequest(

		@NotBlank
		@Size(max = 120)
		String contactName,

		@NotBlank
		@Email
		@Size(max = 254)
		String email,

		@NotBlank
		@Pattern(regexp = "^\\+?[0-9]([ -]?[0-9]){6,14}$",
				message = "must contain 7 to 15 digits, optionally starting with + and separated by single spaces or hyphens")
		String phone,

		@FutureOrPresent
		LocalDate weddingStartDate,

		@FutureOrPresent
		LocalDate weddingEndDate,

		@Size(max = 150)
		String destination,

		@Min(1)
		@Max(100000)
		Integer estimatedGuestCount,

		@PositiveOrZero
		@Digits(integer = 10, fraction = 2)
		BigDecimal budgetMin,

		@PositiveOrZero
		@Digits(integer = 10, fraction = 2)
		BigDecimal budgetMax,

		@Size(max = 1000)
		String servicesOfInterest,

		@Size(max = 200)
		String themeStyle,

		@Size(max = 500)
		String functionsRequired,

		@Size(max = 2000)
		String specialRequests,

		@Size(max = 2000)
		String message,

		@FutureOrPresent
		LocalDate preferredConsultationDate
) {

	@AssertTrue(message = "weddingEndDate requires weddingStartDate and must not be before it")
	public boolean isWeddingDateRangeValid() {
		if (weddingEndDate == null) {
			return true;
		}
		return weddingStartDate != null && !weddingEndDate.isBefore(weddingStartDate);
	}

	@AssertTrue(message = "budgetMax must not be less than budgetMin")
	public boolean isBudgetRangeValid() {
		return budgetMin == null || budgetMax == null || budgetMax.compareTo(budgetMin) >= 0;
	}

}
