package com.wams.qualification.dto;

import com.wams.enquiry.EnquiryStatus;
import com.wams.qualification.AgreementStatus;
import com.wams.qualification.BookingDepositStatus;

public record ConversionReadinessResponse(
        Long enquiryId,
        EnquiryStatus enquiryStatus,
        AgreementStatus agreementStatus,
        BookingDepositStatus bookingDepositStatus,
        boolean eligible,
        Long projectId
) {}
