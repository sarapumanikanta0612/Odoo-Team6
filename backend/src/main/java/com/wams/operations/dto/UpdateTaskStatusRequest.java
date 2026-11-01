package com.wams.operations.dto;

import com.wams.operations.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(@NotNull TaskStatus status) {}
