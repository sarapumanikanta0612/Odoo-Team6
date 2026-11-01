package com.wams.qualification;

import com.wams.enquiry.Enquiry;
import com.wams.enquiry.EnquiryAccessService;
import com.wams.enquiry.EnquiryStatus;
import com.wams.entity.User;
import com.wams.exception.BusinessRuleException;
import com.wams.exception.DuplicateResourceException;
import com.wams.exception.ResourceNotFoundException;
import com.wams.qualification.dto.AgreementResponse;
import com.wams.qualification.dto.BookingDepositResponse;
import com.wams.qualification.dto.ConfirmBookingDepositRequest;
import com.wams.qualification.dto.ConversionReadinessResponse;
import com.wams.qualification.dto.SignAgreementRequest;
import com.wams.qualification.dto.UpdateAgreementRequest;
import com.wams.qualification.dto.UpdateBookingDepositRequest;
import com.wams.repository.WeddingProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

/** Stateful, scoped agreement and booking-deposit workflow used by conversion. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QualificationService {

    private final AgreementRepository agreementRepository;
    private final BookingDepositRepository bookingDepositRepository;
    private final WeddingProjectRepository projectRepository;
    private final EnquiryAccessService accessService;

    public AgreementResponse getAgreement(Long enquiryId) {
        accessService.requireAccessible(enquiryId);
        return AgreementResponse.from(agreementRepository.findByEnquiryId(enquiryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Agreement not found for enquiry " + enquiryId)));
    }

    @Transactional
    public AgreementResponse updateAgreement(Long enquiryId, UpdateAgreementRequest request) {
        User recorder = accessService.currentStaff();
        Enquiry enquiry = openEnquiryForUpdate(enquiryId);
        Agreement agreement = agreementRepository.findByEnquiryId(enquiryId).orElse(null);
        return AgreementResponse.from(applyAgreement(enquiry, agreement, request, recorder, false));
    }

    /** Compatibility action: creates SIGNED directly or signs an existing PENDING agreement. */
    @Transactional
    public AgreementResponse signAgreement(Long enquiryId, SignAgreementRequest request) {
        User recorder = accessService.currentStaff();
        Enquiry enquiry = openEnquiryForUpdate(enquiryId);
        Agreement agreement = agreementRepository.findByEnquiryId(enquiryId).orElse(null);
        if (agreement != null && agreement.getStatus() == AgreementStatus.SIGNED) {
            throw new DuplicateResourceException("Agreement is already signed for enquiry " + enquiryId);
        }
        UpdateAgreementRequest update = new UpdateAgreementRequest(
                request.agreementReference(), AgreementStatus.SIGNED,
                request.signedByName(), request.signedAt());
        return AgreementResponse.from(applyAgreement(enquiry, agreement, update, recorder, true));
    }

    public BookingDepositResponse getBookingDeposit(Long enquiryId) {
        accessService.requireAccessible(enquiryId);
        return BookingDepositResponse.from(bookingDepositRepository.findByEnquiryId(enquiryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking deposit not found for enquiry " + enquiryId)));
    }

    @Transactional
    public BookingDepositResponse updateBookingDeposit(
            Long enquiryId,
            UpdateBookingDepositRequest request) {
        User recorder = accessService.currentStaff();
        Enquiry enquiry = openEnquiryForUpdate(enquiryId);
        BookingDeposit deposit = bookingDepositRepository.findByEnquiryId(enquiryId).orElse(null);
        return BookingDepositResponse.from(applyDeposit(enquiry, deposit, request, recorder, false));
    }

    /** Compatibility action: creates PAID directly or confirms an existing PENDING deposit. */
    @Transactional
    public BookingDepositResponse confirmBookingDeposit(
            Long enquiryId,
            ConfirmBookingDepositRequest request) {
        User recorder = accessService.currentStaff();
        Enquiry enquiry = openEnquiryForUpdate(enquiryId);
        BookingDeposit deposit = bookingDepositRepository.findByEnquiryId(enquiryId).orElse(null);
        if (deposit != null && deposit.getStatus() == BookingDepositStatus.PAID) {
            throw new DuplicateResourceException("Booking deposit is already paid for enquiry " + enquiryId);
        }
        UpdateBookingDepositRequest update = new UpdateBookingDepositRequest(
                request.amount(), BookingDepositStatus.PAID, request.paidOn(),
                request.paymentMethod(), request.paymentReference());
        return BookingDepositResponse.from(applyDeposit(enquiry, deposit, update, recorder, true));
    }

    public ConversionReadinessResponse readiness(Long enquiryId) {
        Enquiry enquiry = accessService.requireAccessible(enquiryId);
        AgreementStatus agreementStatus = agreementRepository.findByEnquiryId(enquiryId)
                .map(Agreement::getStatus).orElse(null);
        BookingDepositStatus depositStatus = bookingDepositRepository.findByEnquiryId(enquiryId)
                .map(BookingDeposit::getStatus).orElse(null);
        Long projectId = projectRepository.findByEnquiryId(enquiryId)
                .map(project -> project.getId()).orElse(null);
        boolean eligible = enquiry.getStatus() != EnquiryStatus.CONVERTED
                && enquiry.getStatus() != EnquiryStatus.LOST
                && agreementStatus == AgreementStatus.SIGNED
                && depositStatus == BookingDepositStatus.PAID
                && projectId == null;
        return new ConversionReadinessResponse(
                enquiryId, enquiry.getStatus(), agreementStatus, depositStatus, eligible, projectId);
    }

    private Agreement applyAgreement(
            Enquiry enquiry,
            Agreement agreement,
            UpdateAgreementRequest request,
            User recorder,
            boolean compatibilityAction) {
        AgreementStatus current = agreement == null ? null : agreement.getStatus();
        AgreementStatus target = request.status();
        if (!validAgreementTransition(current, target)) {
            throw new BusinessRuleException("Invalid agreement transition: " + current + " -> " + target);
        }
        if (current == AgreementStatus.SIGNED && target == AgreementStatus.SIGNED) {
            if (sameSignedAgreement(agreement, request)) return agreement;
            throw new BusinessRuleException("Signed agreement details are immutable; void it instead");
        }
        if (current == AgreementStatus.VOIDED && target == AgreementStatus.VOIDED) return agreement;
        if (compatibilityAction && current == AgreementStatus.VOIDED) {
            throw new BusinessRuleException("A voided agreement cannot be signed");
        }

        String reference = normalizeRequired(request.agreementReference());
        if (compatibilityAction && current == AgreementStatus.PENDING
                && !agreement.getAgreementReference().equals(reference)) {
            throw new BusinessRuleException("Signed agreement reference must match the pending agreement");
        }
        if (target == AgreementStatus.VOIDED && agreement != null
                && !agreement.getAgreementReference().equals(reference)) {
            throw new BusinessRuleException("Agreement reference cannot change while voiding");
        }
        boolean duplicate = agreement == null
                ? agreementRepository.existsByAgreementReference(reference)
                : agreementRepository.existsByAgreementReferenceAndIdNot(reference, agreement.getId());
        if (duplicate) throw new DuplicateResourceException("Agreement reference already exists: " + reference);

        if (target == AgreementStatus.SIGNED
                && (request.signedAt() == null || !hasText(request.signedByName()))) {
            throw new BusinessRuleException("SIGNED agreement requires signedByName and signedAt");
        }
        if (agreement == null) {
            if (target == AgreementStatus.VOIDED) {
                throw new BusinessRuleException("Cannot create an agreement in VOIDED status");
            }
            agreement = new Agreement();
            agreement.setEnquiry(enquiry);
        }
        agreement.setAgreementReference(reference);
        agreement.setStatus(target);
        if (target == AgreementStatus.SIGNED) {
            agreement.setSignedByName(request.signedByName().trim());
            agreement.setSignedAt(request.signedAt());
        } else if (target == AgreementStatus.PENDING) {
            agreement.setSignedByName(null);
            agreement.setSignedAt(null);
        }
        agreement.setRecordedBy(recorder);
        return agreementRepository.save(agreement);
    }

    private BookingDeposit applyDeposit(
            Enquiry enquiry,
            BookingDeposit deposit,
            UpdateBookingDepositRequest request,
            User recorder,
            boolean compatibilityAction) {
        BookingDepositStatus current = deposit == null ? null : deposit.getStatus();
        BookingDepositStatus target = request.status();
        if (!validDepositTransition(current, target)) {
            throw new BusinessRuleException("Invalid booking deposit transition: " + current + " -> " + target);
        }
        if (current == BookingDepositStatus.PAID && target == BookingDepositStatus.PAID) {
            if (samePaidDeposit(deposit, request)) return deposit;
            throw new BusinessRuleException("Paid booking deposit details are immutable; refund it instead");
        }
        if (current == BookingDepositStatus.REFUNDED && target == BookingDepositStatus.REFUNDED) return deposit;
        if (compatibilityAction && current == BookingDepositStatus.REFUNDED) {
            throw new BusinessRuleException("A refunded booking deposit cannot be confirmed again");
        }
        if (deposit == null && target == BookingDepositStatus.REFUNDED) {
            throw new BusinessRuleException("Cannot create a booking deposit in REFUNDED status");
        }
        if (target == BookingDepositStatus.PAID
                && (request.paidOn() == null || request.paymentMethod() == null
                || !hasText(request.paymentReference()))) {
            throw new BusinessRuleException(
                    "PAID booking deposit requires paidOn, paymentMethod and paymentReference");
        }

        if (compatibilityAction && current == BookingDepositStatus.PENDING
                && deposit.getAmount().compareTo(request.amount()) != 0) {
            throw new BusinessRuleException("Paid amount must match the pending booking deposit amount");
        }
        if (target == BookingDepositStatus.REFUNDED
                && deposit != null && deposit.getAmount().compareTo(request.amount()) != 0) {
            throw new BusinessRuleException("Booking deposit amount cannot change while refunding");
        }
        String reference = target == BookingDepositStatus.REFUNDED && deposit != null
                ? deposit.getPaymentReference()
                : hasText(request.paymentReference())
                    ? normalizeRequired(request.paymentReference())
                    : null;
        if (reference != null) {
            boolean duplicate = deposit == null
                    ? bookingDepositRepository.existsByPaymentReference(reference)
                    : bookingDepositRepository.existsByPaymentReferenceAndIdNot(reference, deposit.getId());
            if (duplicate) throw new DuplicateResourceException("Payment reference already exists: " + reference);
        }
        if (deposit == null) {
            deposit = new BookingDeposit();
            deposit.setEnquiry(enquiry);
        }
        if (target != BookingDepositStatus.REFUNDED) deposit.setAmount(request.amount());
        deposit.setStatus(target);
        if (target == BookingDepositStatus.PENDING) {
            deposit.setPaidOn(null);
            deposit.setPaymentMethod(null);
            deposit.setPaymentReference(null);
        } else if (target == BookingDepositStatus.PAID) {
            deposit.setPaidOn(request.paidOn());
            deposit.setPaymentMethod(request.paymentMethod());
            deposit.setPaymentReference(reference);
        }
        // PAID -> REFUNDED preserves the immutable received-payment evidence.
        deposit.setRecordedBy(recorder);
        return bookingDepositRepository.save(deposit);
    }

    private Enquiry openEnquiryForUpdate(Long enquiryId) {
        Enquiry enquiry = accessService.requireAccessibleForUpdate(enquiryId);
        if (enquiry.getStatus() == EnquiryStatus.CONVERTED) {
            throw new BusinessRuleException("Enquiry is already converted: " + enquiryId);
        }
        if (enquiry.getStatus() == EnquiryStatus.LOST) {
            throw new BusinessRuleException("A lost enquiry cannot be qualified: " + enquiryId);
        }
        return enquiry;
    }

    private static boolean validAgreementTransition(AgreementStatus current, AgreementStatus target) {
        if (current == null) return target == AgreementStatus.PENDING || target == AgreementStatus.SIGNED;
        return switch (current) {
            case PENDING -> target == AgreementStatus.PENDING
                    || target == AgreementStatus.SIGNED || target == AgreementStatus.VOIDED;
            case SIGNED -> target == AgreementStatus.SIGNED || target == AgreementStatus.VOIDED;
            case VOIDED -> target == AgreementStatus.VOIDED;
        };
    }

    private static boolean validDepositTransition(BookingDepositStatus current, BookingDepositStatus target) {
        if (current == null) return target == BookingDepositStatus.PENDING || target == BookingDepositStatus.PAID;
        return switch (current) {
            case PENDING -> target == BookingDepositStatus.PENDING || target == BookingDepositStatus.PAID;
            case PAID -> target == BookingDepositStatus.PAID || target == BookingDepositStatus.REFUNDED;
            case REFUNDED -> target == BookingDepositStatus.REFUNDED;
        };
    }

    private static boolean sameSignedAgreement(Agreement agreement, UpdateAgreementRequest request) {
        return agreement.getAgreementReference().equals(normalizeRequired(request.agreementReference()))
                && Objects.equals(agreement.getSignedByName(), trimToNull(request.signedByName()))
                && Objects.equals(agreement.getSignedAt(), request.signedAt());
    }

    private static boolean samePaidDeposit(BookingDeposit deposit, UpdateBookingDepositRequest request) {
        return deposit.getAmount().compareTo(request.amount()) == 0
                && Objects.equals(deposit.getPaidOn(), request.paidOn())
                && deposit.getPaymentMethod() == request.paymentMethod()
                && Objects.equals(deposit.getPaymentReference(),
                hasText(request.paymentReference()) ? normalizeRequired(request.paymentReference()) : null);
    }

    private static String normalizeRequired(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
