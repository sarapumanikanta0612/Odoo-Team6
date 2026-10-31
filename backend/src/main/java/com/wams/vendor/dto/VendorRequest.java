package com.wams.vendor.dto;

import com.wams.vendor.VendorCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body for admin create (POST) and update (PUT). Status is not editable here. */
public record VendorRequest(
        @NotBlank @Size(max = 150) String businessName,
        @NotNull VendorCategory category,
        @Size(max = 100) String contactPerson,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank
        @Pattern(regexp = "^[0-9+()\\- ]{7,20}$", message = "must be a valid phone number")
        String phone,
        @Size(max = 100) String city,
        @Size(max = 255) String address,
        @Size(max = 2000) String serviceDescription
) {
}
