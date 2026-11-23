package com.quickmart.repository;

import com.quickmart.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // The heart of safe rotation: check AND revoke in ONE statement.
    // Two concurrent refreshes with the same token both run this UPDATE; Postgres row-locks the row,
    // the second waits, re-checks "revoked_at IS NULL" after the first commits, and updates 0 rows.
    // A "find, check in Java, then save" version would let both pass the check (same race as stock).
    // clearAutomatically: the persistence context may hold a stale copy of this row; drop it.
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE RefreshToken t SET t.revokedAt = :now
            WHERE t.tokenHash = :hash AND t.revokedAt IS NULL AND t.expiresAt > :now
            """)
    int revokeIfActive(@Param("hash") String tokenHash, @Param("now") Instant now);

    // Logout and reuse detection: kill every still-active token of the session.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now WHERE t.familyId = :family AND t.revokedAt IS NULL")
    int revokeFamily(@Param("family") UUID familyId, @Param("now") Instant now);
}
