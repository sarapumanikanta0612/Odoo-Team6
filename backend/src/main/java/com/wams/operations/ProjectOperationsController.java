package com.wams.operations;

import com.wams.operations.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','LEAD_PLANNER')")
public class ProjectOperationsController {
    private final ProjectOperationsService service;

    @GetMapping("/operations")
    public OperationsResponse operations(@PathVariable Long projectId) { return service.getOperations(projectId); }

    @PostMapping("/functions") @ResponseStatus(HttpStatus.CREATED)
    public FunctionResponse createFunction(@PathVariable Long projectId, @Valid @RequestBody CreateFunctionRequest request) {
        return service.createFunction(projectId, request);
    }

    @PostMapping("/tasks") @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@PathVariable Long projectId, @Valid @RequestBody CreateTaskRequest request) {
        return service.createTask(projectId, request);
    }
}
