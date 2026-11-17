-- Inventory is the many-to-many join between stores and products, with data on it:
-- "store S has N units of product P".

CREATE TABLE inventory (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_id   BIGINT      NOT NULL REFERENCES stores (id),
    product_id BIGINT      NOT NULL REFERENCES products (id),
    -- Last line of defence: even if app code has a bug, stock can never go negative.
    quantity   INT         NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    -- For optimistic locking (@Version) in the concurrency step. Unused until then.
    version    BIGINT      NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- One row per (store, product). Its index (store_id first) also serves "all stock in store S".
    CONSTRAINT uq_inventory_store_product UNIQUE (store_id, product_id)
);
-- The unique index above can't help "which stores have product P?" (product_id is its 2nd column).
CREATE INDEX idx_inventory_product_id ON inventory (product_id);
