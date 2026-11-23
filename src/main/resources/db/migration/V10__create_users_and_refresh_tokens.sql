-- Login identities, separate from the business tables (customers, vendors).
-- See docs/adr/0001-authentication-jwt.md.

CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- Stored lower-cased by the app. The unique index below is on lower(email) anyway,
    -- so "A@x.com" and "a@x.com" can never become two accounts, even if some code forgets.
    email         VARCHAR(255) NOT NULL,
    -- BCrypt output is 60 chars. 100 leaves room for a future algorithm prefix ({argon2}...).
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    customer_id   BIGINT REFERENCES customers (id),
    vendor_id     BIGINT REFERENCES vendors (id),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'VENDOR', 'ADMIN')),
    -- The DB enforces the role <-> link rule: a CUSTOMER always has a customer row and nobody else does.
    -- Same for VENDOR. Java code can't create a "customer" user that orders can't be attached to.
    CONSTRAINT ck_users_customer_link CHECK ((role = 'CUSTOMER') = (customer_id IS NOT NULL)),
    CONSTRAINT ck_users_vendor_link   CHECK ((role = 'VENDOR')   = (vendor_id   IS NOT NULL)),
    -- One login per customer. Vendors may have several staff logins later, so no UNIQUE there.
    CONSTRAINT uq_users_customer_id UNIQUE (customer_id)
);
CREATE UNIQUE INDEX uq_users_email ON users (lower(email));
CREATE INDEX idx_users_vendor_id ON users (vendor_id);   -- FK check when a vendor is deleted

CREATE TABLE refresh_tokens (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- CASCADE: a token has no meaning without its user.
    user_id        BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- One family = one login session. Every rotation stays in the family,
    -- so a stolen token's reuse can revoke the whole session at once.
    family_id      UUID        NOT NULL,
    -- SHA-256 hex of the token. The raw token is never stored: a DB dump can't be replayed.
    token_hash     VARCHAR(64) NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    revoked_at     TIMESTAMPTZ,              -- NULL = still usable
    replaced_by_id BIGINT REFERENCES refresh_tokens (id),   -- audit trail of the rotation chain
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)   -- also the lookup index on every refresh
);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);  -- "revoke this session"
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);      -- FK cascade, "my sessions"
