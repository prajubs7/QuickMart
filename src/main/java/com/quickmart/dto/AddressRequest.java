package com.quickmart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// No customerId: the owner is whoever the access token says. Sizes match the V3 columns.
public record AddressRequest(
        @NotBlank @Size(max = 50) String label,
        @NotBlank @Size(max = 255) String line1,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "must be 6 digits") String pincode,
        boolean isDefault
) {
}
