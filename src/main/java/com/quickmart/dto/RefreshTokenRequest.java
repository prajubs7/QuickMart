package com.quickmart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Body of /api/auth/refresh and /api/auth/logout.
// In the body, not a query param: URLs end up in access logs and proxies.
public record RefreshTokenRequest(@NotBlank @Size(max = 100) String refreshToken) {

    @Override
    public String toString() {
        return "RefreshTokenRequest[refreshToken=***]";
    }
}
