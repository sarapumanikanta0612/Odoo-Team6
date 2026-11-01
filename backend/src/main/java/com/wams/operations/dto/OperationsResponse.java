package com.wams.operations.dto;

import java.util.List;

public record OperationsResponse(List<FunctionResponse> functions, List<TaskResponse> tasks,
                                 int completedTasks, int totalTasks, int completionPercentage) {}
