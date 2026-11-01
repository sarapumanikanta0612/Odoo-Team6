package com.wams.qualification;

import com.wams.qualification.dto.AgreementResponse;
import com.wams.qualification.dto.BookingDepositResponse;
import com.wams.qualification.dto.ConfirmBookingDepositRequest;
import com.wams.qualification.dto.ConversionReadinessResponse;
import com.wams.qualification.dto.SignAgreementRequest;
import com.wams.qualification.dto.UpdateAgreementRequest;
import com.wams.qualification.dto.UpdateBookingDepositRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enquiries/{enquiryId}")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'LEAD_PLANNER')")
public class QualificationController {

    private final QualificationService qualificationService;

    @GetMapping("/agreement")
    public AgreementResponse getAgreement(@PathVariable Long enquiryId) {
        return qualificationService.getAgreement(enquiryId);
    }

    @PutMapping("/agreement")
    public AgreementResponse updateAgreement(
            @PathVariable Long enquiryId,
            @Valid @RequestBody UpdateAgreementRequest request) {
        return qualificationService.updateAgreement(enquiryId, request);
    }

    @PostMapping("/agreement/sign")
    @ResponseStatus(HttpStatus.CREATED)
    public AgreementResponse signAgreement(
            @PathVariable Long enquiryId,
            @Valid @RequestBody SignAgreementRequest request) {
        return qualificationService.signAgreement(enquiryId, request);
    }

    @GetMapping("/booking-deposit")
    public BookingDepositResponse getBookingDeposit(@PathVariable Long enquiryId) {
        return qualificationService.getBookingDeposit(enquiryId);
    }

    @PutMapping("/booking-deposit")
    public BookingDepositResponse updateBookingDeposit(
            @PathVariable Long enquiryId,
            @Valid @RequestBody UpdateBookingDepositRequest request) {
        return qualificationService.updateBookingDeposit(enquiryId, request);
    }

    @PostMapping("/booking-deposit/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDepositResponse confirmBookingDeposit(
            @PathVariable Long enquiryId,
            @Valid @RequestBody ConfirmBookingDepositRequest request) {
        return qualificationService.confirmBookingDeposit(enquiryId, request);
    }

    @GetMapping("/conversion-readiness")
    public ConversionReadinessResponse readiness(@PathVariable Long enquiryId) {
        return qualificationService.readiness(enquiryId);
    }
}
