package com.wams.operations.dto;

import com.wams.operations.ProjectTask;
import com.wams.operations.TaskStatus;
import java.time.LocalDate;

public record TaskResponse(Long id, Long projectId, String projectName, Long functionId, String functionName,
                           Long assignedEmployeeId, String assignedEmployeeName, String title,
                           String description, LocalDate dueDate, TaskStatus status) {
    public static TaskResponse from(ProjectTask t) {
        return new TaskResponse(t.getId(), t.getProject().getId(), t.getProject().getProjectName(),
                t.getFunction() == null ? null : t.getFunction().getId(),
                t.getFunction() == null ? null : t.getFunction().getName(),
                t.getAssignedEmployee().getId(), t.getAssignedEmployee().getUser().getFullName(),
                t.getTitle(), t.getDescription(), t.getDueDate(), t.getStatus());
    }
}
