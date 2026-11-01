package com.wams.enquiry.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

public record CreateFollowUpRequest(
        @NotNull @PastOrPresent Instant occurredAt,
        @NotBlank @Size(max = 2000) String notes,
        @FutureOrPresent LocalDate nextFollowUpDate
) {}
