package com.wams.service;

import com.wams.dto.request.ConvertEnquiryRequest;
import com.wams.dto.request.CreateProjectRequest;
import com.wams.dto.request.UpdateProjectRequest;
import com.wams.dto.response.ProjectResponse;

import java.util.List;

public interface WeddingProjectService {

    /** Create an official project only from persisted signed agreement and paid deposit records. */
    ProjectResponse create(CreateProjectRequest request);

    /** Convert an assigned enquiry; lead ownership is derived from its assignment. */
    ProjectResponse convert(Long enquiryId, ConvertEnquiryRequest request);

    ProjectResponse getById(Long id);

    List<ProjectResponse> list();

    ProjectResponse update(Long id, UpdateProjectRequest request);
}
