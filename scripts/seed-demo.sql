-- Minimal demo data: 1 category, 1 product, 1 vendor + store, 1 customer, stock of 100.
-- NOT a Flyway migration: demo rows don't belong in the schema history, and prod must never get them.
-- Run it in pgAdmin (Query Tool) against the "quickmart" database on localhost:5432.
-- Safe to re-run: ON CONFLICT DO NOTHING skips rows that already exist.
-- Ids are GENERATED ALWAYS, so we can't hard-code them; we look rows up by their unique keys instead.

INSERT INTO categories (name) VALUES ('Dairy') ON CONFLICT DO NOTHING;

INSERT INTO products (sku, name, price, category_id)
SELECT 'MILK-1L', 'Milk 1L', 60.00, id FROM categories WHERE lower(name) = 'dairy'
ON CONFLICT DO NOTHING;

INSERT INTO vendors (name, email) VALUES ('Fresh Mart', 'vendor@freshmart.test') ON CONFLICT DO NOTHING;

INSERT INTO stores (vendor_id, name, city, pincode)
SELECT id, 'Fresh Mart Koramangala', 'Bengaluru', '560034' FROM vendors WHERE email = 'vendor@freshmart.test'
  AND NOT EXISTS (SELECT 1 FROM stores WHERE name = 'Fresh Mart Koramangala');

INSERT INTO customers (name, email, phone) VALUES ('Test Customer', 'customer@test.test', '9999900000')
ON CONFLICT DO NOTHING;

INSERT INTO inventory (store_id, product_id, quantity)
SELECT s.id, p.id, 100 FROM stores s, products p
WHERE s.name = 'Fresh Mart Koramangala' AND p.sku = 'MILK-1L'
ON CONFLICT DO NOTHING;

-- The ids you need for POST /api/orders:
SELECT (SELECT id FROM customers WHERE email = 'customer@test.test') AS customer_id,
       (SELECT id FROM stores    WHERE name  = 'Fresh Mart Koramangala') AS store_id,
       (SELECT id FROM products  WHERE sku   = 'MILK-1L') AS product_id;
