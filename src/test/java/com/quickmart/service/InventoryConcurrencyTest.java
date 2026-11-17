package com.quickmart.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Scenario 6: 50 buyers race for the last 10 units.
 *
 * NOT @Transactional on purpose: a test transaction would wrap everything in ONE
 * connection and roll back at the end. We need 50 real, separate transactions that commit.
 * So we insert our own rows and delete them afterwards.
 *
 * Runs against the dev database (same as contextLoads), so docker compose must be up.
 */
@SpringBootTest
class InventoryConcurrencyTest {

    private static final int STOCK = 10;
    private static final int BUYERS = 50;

    @Autowired InventoryService inventoryService;
    @Autowired JdbcTemplate jdbc;   // plain SQL for setup/assertions, so JPA caching can't fool us

    private long vendorId, storeId, categoryId, productId;

    @BeforeEach
    void setUp() {
        String tag = UUID.randomUUID().toString().substring(0, 8); // unique sku/email/name per run
        vendorId = jdbc.queryForObject(
                "INSERT INTO vendors (name, email) VALUES (?, ?) RETURNING id",
                Long.class, "race-vendor-" + tag, tag + "@race.test");
        storeId = jdbc.queryForObject(
                "INSERT INTO stores (vendor_id, name, city, pincode) VALUES (?, ?, 'Test', '000000') RETURNING id",
                Long.class, vendorId, "race-store-" + tag);
        categoryId = jdbc.queryForObject(
                "INSERT INTO categories (name) VALUES (?) RETURNING id",
                Long.class, "race-cat-" + tag);
        productId = jdbc.queryForObject(
                "INSERT INTO products (sku, name, price, category_id) VALUES (?, 'Last-item phone', 999.00, ?) RETURNING id",
                Long.class, "RACE-" + tag, categoryId);
        jdbc.update("INSERT INTO inventory (store_id, product_id, quantity) VALUES (?, ?, ?)",
                storeId, productId, STOCK);
    }

    @AfterEach
    void cleanUp() {
        // Children first, because of the foreign keys.
        jdbc.update("DELETE FROM inventory WHERE product_id = ?", productId);
        jdbc.update("DELETE FROM products WHERE id = ?", productId);
        jdbc.update("DELETE FROM categories WHERE id = ?", categoryId);
        jdbc.update("DELETE FROM stores WHERE id = ?", storeId);
        jdbc.update("DELETE FROM vendors WHERE id = ?", vendorId);
    }

    @Test
    void fiftyBuyersCannotBuyMoreThanTenItems() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(BUYERS);
        // Every thread waits at this gate, then all are released at once -> maximum overlap.
        CountDownLatch startGate = new CountDownLatch(1);

        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger outOfStock = new AtomicInteger();
        List<Throwable> otherErrors = new CopyOnWriteArrayList<>();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < BUYERS; i++) {
            futures.add(pool.submit(() -> {
                startGate.await();
                try {
                    inventoryService.reserve(storeId, productId, 1);
                    succeeded.incrementAndGet();
                } catch (com.quickmart.exception.InsufficientStockException e) {
                    outOfStock.incrementAndGet();
                } catch (Throwable e) {
                    otherErrors.add(e); // after your fix, lock failures will land here: decide how to treat them
                }
                return null;
            }));
        }

        startGate.countDown();
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        int left = jdbc.queryForObject(
                "SELECT quantity FROM inventory WHERE store_id = ? AND product_id = ?",
                Integer.class, storeId, productId);

        System.out.printf("%n=== RACE RESULT: sold=%d, outOfStock=%d, otherErrors=%d, stockLeft=%d (started with %d) ===%n%n",
                succeeded.get(), outOfStock.get(), otherErrors.size(), left, STOCK);
        otherErrors.stream().limit(3).forEach(e -> System.out.println("  error: " + e));

        // The business rule: units sold + units left == units we had. Never sell what we don't have.
        assertEquals(STOCK, succeeded.get(), "units sold");
        assertEquals(0, left, "stock left");
        assertEquals(STOCK, succeeded.get() + left, "sold + left must equal starting stock");
    }
}
