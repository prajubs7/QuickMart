package com.quickmart.service;

import com.quickmart.dto.CategoryResponse;
import com.quickmart.dto.PageResponse;
import com.quickmart.dto.ProductResponse;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Cache-aside (a.k.a. lazy loading) for catalog reads:
 *   1. look in Redis
 *   2. hit  -> return it, Postgres is never touched
 *   3. miss -> load from Postgres, store in Redis with a TTL, return it
 *
 * Why a separate class instead of caching inside ProductService:
 * ProductService is @Transactional(readOnly = true). Spring opens that transaction, and borrows
 * a DB connection from the pool, BEFORE the method body runs. A cache hit in there would still
 * hold a connection. Here there's no transaction, so a hit costs zero DB work.
 *
 * Note what's NOT here: nothing removes entries when a product is updated.
 */
@Slf4j
@Service
public class CatalogCacheService {

    // Jackson needs the full generic type (PageResponse<ProductResponse>, not just PageResponse)
    // to rebuild the right objects from JSON. An anonymous subclass captures it.
    private static final TypeReference<ProductResponse> PRODUCT = new TypeReference<>() {};
    private static final TypeReference<PageResponse<ProductResponse>> PRODUCT_PAGE = new TypeReference<>() {};
    private static final TypeReference<PageResponse<CategoryResponse>> CATEGORY_PAGE = new TypeReference<>() {};

    private final StringRedisTemplate redis;
    private final JsonMapper json;
    private final MeterRegistry meters;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final boolean enabled;
    private final Duration ttl;

    public CatalogCacheService(StringRedisTemplate redis, JsonMapper json, MeterRegistry meters,
                               ProductService productService, CategoryService categoryService,
                               @Value("${quickmart.cache.enabled}") boolean enabled,
                               @Value("${quickmart.cache.ttl}") Duration ttl) {
        this.redis = redis;
        this.json = json;
        this.meters = meters;
        this.productService = productService;
        this.categoryService = categoryService;
        this.enabled = enabled;
        this.ttl = ttl;
    }

    public ProductResponse getProduct(Long id) {
        return getOrLoad("product", "catalog:product:" + id, PRODUCT,
                () -> productService.get(id));
    }

    // Every parameter that changes the result must be in the key. Forget "size" and
    // ?size=5 and ?size=50 would share one entry: the second caller gets the wrong page.
    public PageResponse<ProductResponse> listProducts(Long categoryId, Pageable pageable) {
        String key = "catalog:products:cat=" + (categoryId == null ? "all" : categoryId) + ":" + pageKey(pageable);
        return getOrLoad("products", key, PRODUCT_PAGE,
                () -> productService.list(categoryId, pageable));
    }

    public PageResponse<CategoryResponse> listCategories(Pageable pageable) {
        return getOrLoad("categories", "catalog:categories:" + pageKey(pageable), CATEGORY_PAGE,
                () -> categoryService.list(pageable));
    }

    private <T> T getOrLoad(String cacheName, String key, TypeReference<T> type, Supplier<T> loader) {
        if (!enabled) {
            return loader.get();
        }

        String cached;
        try {
            cached = redis.opsForValue().get(key);
        } catch (RuntimeException e) {
            // Redis is an optimization, not the source of truth. If it's down, serve from Postgres
            // (slower) instead of failing the request.
            log.warn("Cache read failed for {}, falling back to DB: {}", key, e.getMessage());
            count(cacheName, "error");
            return loader.get();
        }

        if (cached != null) {
            count(cacheName, "hit");
            return json.readValue(cached, type);
        }

        count(cacheName, "miss");
        T value = loader.get(); // throws 404 for a missing product -> nothing gets cached
        try {
            // SET key value EX <ttl>, one command. The TTL is the safety net: even if we never
            // invalidate, a stale entry dies on its own after ttl.
            redis.opsForValue().set(key, json.writeValueAsString(value), ttl);
        } catch (RuntimeException e) {
            log.warn("Cache write failed for {}: {}", key, e.getMessage());
        }
        return value;
    }

    // Shows up at /metrics/quickmart.cache.requests?tag=cache:products&tag=result:hit
    private void count(String cacheName, String result) {
        meters.counter("quickmart.cache.requests", "cache", cacheName, "result", result).increment();
    }

    private static String pageKey(Pageable p) {
        // Sort.toString() is like "price: DESC,id: ASC". Strip spaces to keep keys tidy.
        return "page=" + p.getPageNumber() + ":size=" + p.getPageSize()
                + ":sort=" + p.getSort().toString().replace(" ", "");
    }
}
