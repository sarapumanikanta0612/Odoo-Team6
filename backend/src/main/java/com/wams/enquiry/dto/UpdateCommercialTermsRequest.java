package com.wams.enquiry.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateCommercialTermsRequest(
        @NotBlank @Size(max = 200) String selectedPackage,
        @Size(max = 1000) String packageDescription,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 12, fraction = 2)
        BigDecimal agreedAmount
) {}
