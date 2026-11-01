package com.wams.vendor.dto;

import com.wams.vendor.Vendor;
import com.wams.vendor.VendorCategory;
import com.wams.vendor.VendorStatus;

import java.time.Instant;

public record VendorResponse(
        Long id,
        Long userId,
        String businessName,
        VendorCategory category,
        String contactPerson,
        String email,
        String phone,
        String city,
        String address,
        String serviceDescription,
        VendorStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static VendorResponse from(Vendor vendor) {
        return new VendorResponse(
                vendor.getId(),
                vendor.getUser() == null ? null : vendor.getUser().getId(),
                vendor.getBusinessName(),
                vendor.getCategory(),
                vendor.getContactPerson(),
                vendor.getEmail(),
                vendor.getPhone(),
                vendor.getCity(),
                vendor.getAddress(),
                vendor.getServiceDescription(),
                vendor.getStatus(),
                vendor.getCreatedAt(),
                vendor.getUpdatedAt());
    }
}
