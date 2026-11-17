CREATE TABLE customers (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_customers_email UNIQUE (email),
    CONSTRAINT uq_customers_phone UNIQUE (phone)   -- quick-commerce apps log in by phone/OTP
);

CREATE TABLE addresses (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- CASCADE here (unlike stores): an address has no meaning without its customer.
    customer_id BIGINT       NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    label       VARCHAR(50)  NOT NULL,          -- "Home", "Work"
    line1       VARCHAR(255) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    pincode     VARCHAR(10)  NOT NULL,
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_addresses_customer_id ON addresses (customer_id);  -- "my addresses"
-- Partial unique index: at most ONE default address per customer.
-- Only rows WHERE is_default are indexed, so the rule is enforced by the DB, not by hopeful Java code.
CREATE UNIQUE INDEX uq_addresses_one_default ON addresses (customer_id) WHERE is_default;
