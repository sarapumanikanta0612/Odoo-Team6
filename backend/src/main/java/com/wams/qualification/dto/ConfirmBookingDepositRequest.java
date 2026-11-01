package com.wams.qualification.dto;

import com.wams.qualification.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConfirmBookingDepositRequest(
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 12, fraction = 2)
        BigDecimal amount,
        @NotNull @PastOrPresent LocalDate paidOn,
        @NotNull PaymentMethod paymentMethod,
        @NotBlank @Size(max = 100) String paymentReference
) {}
