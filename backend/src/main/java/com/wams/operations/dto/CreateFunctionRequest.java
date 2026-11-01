package com.wams.operations.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateFunctionRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull LocalDate functionDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotBlank @Size(max = 300) String venue
) {}
