-- One row per charge the gateway accepted.
-- Deliberately NO uniqueness yet: nothing stops two rows for the same checkout (scenario 2).
CREATE TABLE payments (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id    BIGINT         NOT NULL REFERENCES orders (id),
    amount      NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    status      VARCHAR(20)    NOT NULL,
    -- The gateway's own id for the charge. Support uses it to find/refund the charge at the provider.
    gateway_ref VARCHAR(64)    NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX idx_payments_order_id ON payments (order_id); -- "payments of order X"
