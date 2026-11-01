package com.wams.operations.dto;

import com.wams.operations.WeddingFunction;
import java.time.*;

public record FunctionResponse(Long id, Long projectId, String name, LocalDate functionDate,
                               LocalTime startTime, LocalTime endTime, String venue) {
    public static FunctionResponse from(WeddingFunction f) {
        return new FunctionResponse(f.getId(), f.getProject().getId(), f.getName(), f.getFunctionDate(), f.getStartTime(), f.getEndTime(), f.getVenue());
    }
}
