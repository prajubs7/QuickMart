package com.quickmart.service;

import com.quickmart.config.JwtClaims;
import com.quickmart.config.JwtProperties;
import com.quickmart.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

// Issues short-lived access tokens. Verifying them is Spring's job (JwtConfig.jwtDecoder).
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtEncoder encoder;
    private final JwtProperties props;

    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(props.accessTokenTtl()))
                .claim(JwtClaims.ROLE, user.getRole().name());
        // Only what authorization needs. A JWT is signed, NOT encrypted: anyone holding it can read it,
        // so no email, phone or name in here.
        if (user.getCustomerId() != null) claims.claim(JwtClaims.CUSTOMER_ID, user.getCustomerId());
        if (user.getVendorId() != null) claims.claim(JwtClaims.VENDOR_ID, user.getVendorId());

        // The header must name HS256 explicitly; the encoder's default is RS256, which our key can't sign.
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    public long accessTokenTtlSeconds() {
        return props.accessTokenTtl().toSeconds();
    }
}
