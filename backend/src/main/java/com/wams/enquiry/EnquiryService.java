package com.wams.enquiry;

import com.wams.enquiry.dto.EnquiryResponse;
import com.wams.enquiry.dto.PublicEnquiryRequest;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnquiryService {

	private final EnquiryRepository enquiryRepository;

	/**
	 * Records an enquiry submitted through the public website form.
	 * <p>
	 * The status is always NEW and the source is always WEBSITE. The request has no
	 * field that could change either of them.
	 */
	@Transactional
	public EnquiryResponse createPublicEnquiry(PublicEnquiryRequest request) {
		Enquiry enquiry = new Enquiry();
		enquiry.setContactName(request.contactName().trim());
		enquiry.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
		enquiry.setPhone(request.phone().trim());
		enquiry.setWeddingStartDate(request.weddingStartDate());
		enquiry.setWeddingEndDate(request.weddingEndDate());
		enquiry.setDestination(trimToNull(request.destination()));
		enquiry.setEstimatedGuestCount(request.estimatedGuestCount());
		enquiry.setBudgetMin(request.budgetMin());
		enquiry.setBudgetMax(request.budgetMax());
		enquiry.setServicesOfInterest(trimToNull(request.servicesOfInterest()));
		enquiry.setThemeStyle(trimToNull(request.themeStyle()));
		enquiry.setFunctionsRequired(trimToNull(request.functionsRequired()));
		enquiry.setSpecialRequests(trimToNull(request.specialRequests()));
		enquiry.setMessage(trimToNull(request.message()));
		enquiry.setPreferredConsultationDate(request.preferredConsultationDate());
		enquiry.setStatus(EnquiryStatus.NEW);
		enquiry.setSource(EnquirySource.WEBSITE);
		return EnquiryResponse.from(enquiryRepository.save(enquiry));
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

}
