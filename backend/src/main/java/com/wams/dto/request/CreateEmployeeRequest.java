package com.wams.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateEmployeeRequest(
        @NotNull Long userId,
        @NotBlank @Size(max = 50) String employeeCode,
        @Size(max = 100) String designation,
        @Size(max = 100) String department,
        LocalDate joiningDate,
        Boolean active
) {}
