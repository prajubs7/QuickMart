package com.quickmart.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

// quickmart.security.jwt.* from application.yml. @Validated: a missing value fails startup, not the first login.
@Validated
@ConfigurationProperties("quickmart.security.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotBlank String issuer,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration refreshTokenTtl
) {
    public JwtProperties {
        // HS256 needs a key of at least 256 bits. A short secret can be brute-forced offline
        // from any single token, and then the attacker can mint ADMIN tokens.
        if (secret != null && secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("quickmart.security.jwt.secret must be at least 32 bytes");
        }
    }
}
