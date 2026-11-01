package com.wams.operations.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CreateTaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 1000) String description,
        Long functionId,
        @NotNull @Positive Long assignedEmployeeId,
        @NotNull LocalDate dueDate
) {}
