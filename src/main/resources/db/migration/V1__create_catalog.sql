-- Catalog: one global product list shared by every store (the "dark store" model).
-- A vendor doesn't own products. A vendor runs stores, and stores stock catalog products (see inventory).

CREATE TABLE categories (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- Unique on lower(name), so "Fruits" and "fruits" can't both exist.
-- This also serves as the index for case-insensitive lookups by name.
CREATE UNIQUE INDEX uq_categories_name_lower ON categories (lower(name));

CREATE TABLE products (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku         VARCHAR(50)    NOT NULL,
    name        VARCHAR(200)   NOT NULL,
    description TEXT,
    -- NUMERIC, never FLOAT/DOUBLE: 0.1 + 0.2 must equal 0.3 when you're charging money
    price       NUMERIC(10, 2) NOT NULL CHECK (price > 0),
    category_id BIGINT         NOT NULL REFERENCES categories (id),
    active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_products_sku UNIQUE (sku)   -- the business key; the unique constraint also creates its index
);
-- Postgres does NOT index foreign keys automatically.
-- Needed for "products in category X" and for the FK check when a category is deleted.
CREATE INDEX idx_products_category_id ON products (category_id);
