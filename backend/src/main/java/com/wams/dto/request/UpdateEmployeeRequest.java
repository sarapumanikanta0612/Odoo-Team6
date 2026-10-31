package com.wams.dto.request;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateEmployeeRequest(
        @Size(max = 100) String designation,
        @Size(max = 100) String department,
        LocalDate joiningDate,
        Boolean active
) {}
