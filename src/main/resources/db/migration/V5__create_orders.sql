-- "order" is a reserved word in SQL, so the table is "orders".

CREATE TABLE orders (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id  BIGINT         NOT NULL REFERENCES customers (id),
    store_id     BIGINT         NOT NULL REFERENCES stores (id),
    status       VARCHAR(20)    NOT NULL,
    -- 12 digits, not 10: an order total is a sum of many prices and can exceed one product's max.
    total_amount NUMERIC(12, 2) NOT NULL CHECK (total_amount >= 0),
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now()
);
-- "order history of customer X". Revisited with created_at in the large-data steps.
CREATE INDEX idx_orders_customer_id ON orders (customer_id);

CREATE TABLE order_items (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- CASCADE: order items have no meaning without their order.
    order_id   BIGINT         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id BIGINT         NOT NULL REFERENCES products (id),
    quantity   INT            NOT NULL CHECK (quantity > 0),
    -- Price at the moment of ordering. products.price can change later; the bill must not.
    unit_price NUMERIC(10, 2) NOT NULL
);
CREATE INDEX idx_order_items_order_id ON order_items (order_id);     -- "items of order X"
CREATE INDEX idx_order_items_product_id ON order_items (product_id); -- FK check when a product is deleted
