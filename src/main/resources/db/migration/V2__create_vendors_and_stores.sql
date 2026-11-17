-- A vendor is a business (the "multi-vendor" part). One vendor operates many stores.

CREATE TABLE vendors (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_vendors_email UNIQUE (email)
);

CREATE TABLE stores (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- No ON DELETE CASCADE: deleting a vendor must not silently wipe its stores (and their stock).
    vendor_id  BIGINT       NOT NULL REFERENCES vendors (id),
    name       VARCHAR(200) NOT NULL,
    city       VARCHAR(100) NOT NULL,
    pincode    VARCHAR(10)  NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_stores_vendor_id ON stores (vendor_id);  -- "stores of vendor X"
-- "which store serves this pincode?" is asked on every app open
CREATE INDEX idx_stores_pincode ON stores (pincode);
