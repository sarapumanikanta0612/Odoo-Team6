package com.wams.enquiry.dto;

import com.wams.enquiry.EnquiryStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateEnquiryStatusRequest(@NotNull EnquiryStatus status) {}
