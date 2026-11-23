package com.quickmart.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// No "role" field on purpose: register always creates a CUSTOMER.
// If the client could send "role": "ADMIN", anyone could make themselves an admin.
public record RegisterRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Pattern(regexp = "^[0-9]{10,15}$", message = "must be 10-15 digits") String phone,
        // BCrypt only reads the first 72 BYTES. AuthService also checks the byte length (non-ASCII chars).
        @NotBlank @Size(min = 8, max = 72) String password
) {
    // Keep the password out of logs if this record is ever logged or printed in an exception.
    @Override
    public String toString() {
        return "RegisterRequest[name=" + name + ", email=" + email + ", phone=" + phone + ", password=***]";
    }
}
