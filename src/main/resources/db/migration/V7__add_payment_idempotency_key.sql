-- Scenario 2 fix: one payment per client-generated Idempotency-Key.
-- Nullable because rows from before this migration have no key.
-- A Postgres UNIQUE constraint allows any number of NULLs, so those old rows don't conflict.
ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(100);

-- The real guarantee. A Java "does this key exist?" check can be raced by two requests at once;
-- the database cannot. The constraint also creates the index that findByIdempotencyKey uses.
ALTER TABLE payments ADD CONSTRAINT uq_payments_idempotency_key UNIQUE (idempotency_key);
