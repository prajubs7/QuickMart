-- Object-level authorization for vendors (ADR 0002).
--
-- NULL      = platform catalog product (the V1 "dark store" model): managed by ADMIN, any store may stock it.
-- NOT NULL  = a vendor's own product: only that vendor (or an ADMIN) may edit it, only that vendor's stores stock it.
--
-- Nullable on purpose: every existing row is a platform product, so no backfill guess is needed.
ALTER TABLE products ADD COLUMN vendor_id BIGINT REFERENCES vendors (id);

-- "products of vendor X" (vendor dashboard) and the FK check when a vendor is deleted.
-- Partial: platform products (NULL) are never looked up by vendor.
CREATE INDEX idx_products_vendor_id ON products (vendor_id) WHERE vendor_id IS NOT NULL;
