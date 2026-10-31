package com.wams.enquiry;

import com.wams.enquiry.dto.EnquiryResponse;
import com.wams.enquiry.dto.PublicEnquiryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enquiry submission from the public website. Must be reachable without logging in,
 * which depends on the shared security configuration permitting this path.
 */
@RestController
@RequestMapping("/api/public/enquiries")
@RequiredArgsConstructor
public class PublicEnquiryController {

	private final EnquiryService enquiryService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public EnquiryResponse submit(@Valid @RequestBody PublicEnquiryRequest request) {
		return enquiryService.createPublicEnquiry(request);
	}

}
