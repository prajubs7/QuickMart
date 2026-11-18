-- Round B: a payment row is now created as PENDING *before* the gateway is called,
-- so at that moment there is no gateway reference yet.
-- New file instead of editing V6: V6 is already applied, and Flyway would reject
-- the changed checksum at startup.
ALTER TABLE payments ALTER COLUMN gateway_ref DROP NOT NULL;
