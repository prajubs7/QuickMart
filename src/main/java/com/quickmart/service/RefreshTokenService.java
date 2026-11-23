package com.quickmart.service;

import com.quickmart.config.JwtProperties;
import com.quickmart.entity.RefreshToken;
import com.quickmart.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Opaque refresh tokens: random, stored as SHA-256 hashes, rotated on every use, with reuse detection.
 * See docs/adr/0001-authentication-jwt.md, decision 3.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final JwtProperties props;

    public record Rotated(Long userId, UUID familyId, String refreshToken) {
    }

    // Login and register: a brand-new family = a new session.
    @Transactional
    public String startSession(Long userId) {
        return issue(userId, UUID.randomUUID(), Instant.now()).raw();
    }

    /**
     * Swap a refresh token for a new one.
     *
     * noRollbackFor: on reuse we revoke the family and THEN throw 401. By default a RuntimeException
     * rolls the transaction back, which would undo the revocation and let the thief keep the session.
     */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public Rotated rotate(String rawToken) {
        String hash = sha256(rawToken);
        Instant now = Instant.now();

        // Atomic check-and-revoke. Exactly one concurrent caller gets 1 here (see the repository).
        if (repository.revokeIfActive(hash, now) == 1) {
            RefreshToken old = repository.findByTokenHash(hash).orElseThrow();
            Issued next = issue(old.getUserId(), old.getFamilyId(), now);
            old.replacedBy(next.entity());   // managed entity: dirty checking writes replaced_by_id
            return new Rotated(old.getUserId(), old.getFamilyId(), next.raw());
        }

        // 0 rows: unknown, expired, or ALREADY USED. Find out which.
        RefreshToken token = repository.findByTokenHash(hash).orElseThrow(RefreshTokenService::invalid);
        if (token.getRevokedAt() != null) {
            // A rotated token came back. Either it was stolen, or two requests raced with it.
            // We can't tell the real user from the thief, so end the whole session for both.
            int killed = repository.revokeFamily(token.getFamilyId(), now);
            log.warn("Refresh token reuse for user {} (family {}): revoked {} active token(s)",
                    token.getUserId(), token.getFamilyId(), killed);
        }
        throw invalid();
    }

    // Logout. Unknown or already-revoked tokens are fine: logout is idempotent and reveals nothing.
    @Transactional
    public void endSession(String rawToken) {
        repository.findByTokenHash(sha256(rawToken))
                .ifPresent(t -> repository.revokeFamily(t.getFamilyId(), Instant.now()));
    }

    @Transactional
    public void endSession(UUID familyId) {
        repository.revokeFamily(familyId, Instant.now());
    }

    private record Issued(RefreshToken entity, String raw) {
    }

    private Issued issue(Long userId, UUID familyId, Instant now) {
        byte[] bytes = new byte[32];   // 256 bits: guessing one is hopeless, so a fast hash is enough
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken saved = repository.save(
                new RefreshToken(userId, familyId, sha256(raw), now.plus(props.refreshTokenTtl())));
        return new Issued(saved, raw);
    }

    // SHA-256, not BCrypt: we must FIND the row by hash. BCrypt's random salt gives a different
    // hash each time, so no index lookup would work. BCrypt's slowness protects LOW-entropy
    // secrets (passwords); a 256-bit random token doesn't need it.
    static String sha256(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every JVM", e);
        }
    }

    private static BadCredentialsException invalid() {
        return new BadCredentialsException("Invalid or expired refresh token");
    }
}
