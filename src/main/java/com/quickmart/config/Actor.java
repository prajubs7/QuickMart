package com.quickmart.config;

import com.quickmart.entity.Role;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * "Who is calling", built from the verified token and handed to services that must decide
 * object-level access (a vendor editing a product, a vendor changing a store's stock).
 *
 * Services take this instead of a Jwt so they stay free of web/security types and are easy to test.
 * The ids are null unless the role has one (CUSTOMER -> customerId, VENDOR -> vendorId).
 */
public record Actor(Role role, Long customerId, Long vendorId) {

    public static Actor from(Jwt jwt) {
        Role role = Role.valueOf(jwt.getClaimAsString(JwtClaims.ROLE));
        return switch (role) {
            case CUSTOMER -> new Actor(role, JwtClaims.customerId(jwt), null);
            case VENDOR -> new Actor(role, null, JwtClaims.vendorId(jwt));
            case ADMIN -> new Actor(role, null, null);
        };
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    // True only for the vendor whose id matches. null (platform-owned) never matches a vendor.
    public boolean isVendor(Long ownerVendorId) {
        return role == Role.VENDOR && vendorId != null && vendorId.equals(ownerVendorId);
    }
}
