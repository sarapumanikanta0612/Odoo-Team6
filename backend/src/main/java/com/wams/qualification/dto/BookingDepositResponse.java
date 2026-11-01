package com.wams.qualification.dto;

import com.wams.qualification.BookingDeposit;
import com.wams.qualification.BookingDepositStatus;
import com.wams.qualification.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BookingDepositResponse(
        Long id,
        Long enquiryId,
        BookingDepositStatus status,
        BigDecimal amount,
        LocalDate paidOn,
        PaymentMethod paymentMethod,
        String paymentReference,
        Long recordedByUserId,
        Instant createdAt,
        Instant updatedAt
) {
    public static BookingDepositResponse from(BookingDeposit deposit) {
        return new BookingDepositResponse(
                deposit.getId(),
                deposit.getEnquiry().getId(),
                deposit.getStatus(),
                deposit.getAmount(),
                deposit.getPaidOn(),
                deposit.getPaymentMethod(),
                deposit.getPaymentReference(),
                deposit.getRecordedBy().getId(),
                deposit.getCreatedAt(),
                deposit.getUpdatedAt());
    }
}
