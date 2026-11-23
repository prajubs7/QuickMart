package com.quickmart.config;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

// Claim names shared by the token issuer (JwtTokenService) and the readers (controllers, SecurityConfig).
public final class JwtClaims {

    public static final String ROLE = "role";
    public static final String CUSTOMER_ID = "cid";
    public static final String VENDOR_ID = "vid";

    private JwtClaims() {
    }

    // THE way controllers get "who is the customer". Never from the request body or query string:
    // those are typed by the caller. The token is signed by us, so the caller can't change it.
    public static Long customerId(Jwt jwt) {
        if (jwt.getClaim(CUSTOMER_ID) instanceof Number id) {
            return id.longValue();
        }
        throw new AccessDeniedException("Not a customer account");
    }

    // Same rule for vendors: "which vendor am I" comes only from the signed "vid" claim.
    public static Long vendorId(Jwt jwt) {
        if (jwt.getClaim(VENDOR_ID) instanceof Number id) {
            return id.longValue();
        }
        throw new AccessDeniedException("Not a vendor account");
    }
}
