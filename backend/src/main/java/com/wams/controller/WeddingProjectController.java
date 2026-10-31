package com.wams.controller;

import com.wams.dto.request.CreateProjectRequest;
import com.wams.dto.request.UpdateProjectRequest;
import com.wams.dto.response.ProjectResponse;
import com.wams.service.WeddingProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class WeddingProjectController {

    private final WeddingProjectService projectService;

    public WeddingProjectController(WeddingProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(req));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> list() {
        return ResponseEntity.ok(projectService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateProjectRequest req) {
        return ResponseEntity.ok(projectService.update(id, req));
    }
}
