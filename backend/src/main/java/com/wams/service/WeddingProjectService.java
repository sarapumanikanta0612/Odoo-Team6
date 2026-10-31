package com.wams.service;

import com.wams.dto.request.CreateProjectRequest;
import com.wams.dto.request.UpdateProjectRequest;
import com.wams.dto.response.ProjectResponse;

import java.util.List;

public interface WeddingProjectService {

    /**
     * Create an official WeddingProject. The service MUST reject the request
     * if either {@code agreementSigned} or {@code bookingDepositPaid} is false.
     */
    ProjectResponse create(CreateProjectRequest request);

    ProjectResponse getById(Long id);

    List<ProjectResponse> list();

    ProjectResponse update(Long id, UpdateProjectRequest request);
}
