package com.wams.dto.request;

import com.wams.enums.ProjectStatus;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProjectRequest(
        @Size(max = 200) String projectName,
        LocalDate weddingDate,
        @Size(max = 300) String venue,
        ProjectStatus projectStatus,
        Long partnerClientId,
        Long leadPlannerId
) {}
