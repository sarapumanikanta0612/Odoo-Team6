package com.wams.enquiry.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Compare-and-set request: expected current ID protects deliberate relinking. */
public record LinkPrimaryClientRequest(
        @NotNull @Positive Long primaryClientId,
        @Positive Long expectedCurrentPrimaryClientId
) {}
