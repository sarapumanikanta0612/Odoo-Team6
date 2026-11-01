package com.wams.operations;

import com.wams.operations.dto.TaskResponse;
import com.wams.operations.dto.UpdateTaskStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final ProjectOperationsService service;

    @GetMapping("/mine")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<TaskResponse> mine() { return service.myTasks(); }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','LEAD_PLANNER','EMPLOYEE')")
    public TaskResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateTaskStatusRequest request) {
        return service.updateTaskStatus(id, request);
    }
}
