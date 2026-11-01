package com.wams.enquiry.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignEnquiryRequest(@NotNull @Positive Long assignedLeadPlannerId) {}
