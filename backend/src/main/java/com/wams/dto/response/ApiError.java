package com.wams.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Consistent error envelope returned by the global exception handler.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ApiError {
    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final List<String> details;
}
