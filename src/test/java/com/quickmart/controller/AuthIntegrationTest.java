package com.quickmart.controller;

import com.quickmart.service.RefreshTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ADR 0001 end to end: real tokens from /api/auth, real filter chain, real database.
 * Runs against the dev database (same as the other tests), so docker compose must be up.
 * Not @Transactional: the concurrency test needs real, separate, committed transactions.
 */
@SpringBootTest(properties = {
        "quickmart.payment-gateway.delay-ms=0",
        "quickmart.payment-gateway.failure-rate=0.0",
        "quickmart.email.delay-ms=0"
})
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtDecoder jwtDecoder;
    @Autowired RefreshTokenService refreshTokenService;

    private String tag;
    private long vendorId, storeId, categoryId, productId;

    @BeforeEach
    void setUp() {
        tag = UUID.randomUUID().toString().substring(0, 8);
        vendorId = jdbc.queryForObject("INSERT INTO vendors (name, email) VALUES ('V', ?) RETURNING id",
                Long.class, "auth-vendor-" + tag + "@test.test");
        storeId = jdbc.queryForObject(
                "INSERT INTO stores (vendor_id, name, city, pincode) VALUES (?, ?, 'Test', '000000') RETURNING id",
                Long.class, vendorId, "auth-store-" + tag);
        categoryId = jdbc.queryForObject("INSERT INTO categories (name) VALUES (?) RETURNING id",
                Long.class, "auth-cat-" + tag);
        productId = jdbc.queryForObject(
                "INSERT INTO products (sku, name, price, category_id) VALUES (?, 'Milk', 60.00, ?) RETURNING id",
                Long.class, "AUTH-" + tag, categoryId);
        jdbc.update("INSERT INTO inventory (store_id, product_id, quantity) VALUES (?, ?, 100)", storeId, productId);
    }

    @AfterEach
    void cleanUp() {
        String emails = "%-" + tag + "@test.test";
        // Children first, because of the foreign keys. refresh_tokens go with users (ON DELETE CASCADE).
        jdbc.update("""
                DELETE FROM payments WHERE order_id IN (SELECT o.id FROM orders o
                  JOIN customers c ON c.id = o.customer_id WHERE c.email LIKE ?)""", emails);
        jdbc.update("DELETE FROM orders WHERE customer_id IN (SELECT id FROM customers WHERE email LIKE ?)", emails);
        jdbc.update("DELETE FROM users WHERE email LIKE ?", emails);
        jdbc.update("DELETE FROM customers WHERE email LIKE ?", emails);
        jdbc.update("DELETE FROM inventory WHERE product_id = ?", productId);
        jdbc.update("DELETE FROM products WHERE id = ?", productId);
        jdbc.update("DELETE FROM categories WHERE id = ?", categoryId);
        jdbc.update("DELETE FROM stores WHERE id = ?", storeId);
        jdbc.update("DELETE FROM vendors WHERE id = ?", vendorId);
    }

    // ---------- 401 / 403 in the standard ErrorResponse shape ----------

    @Test
    void noToken_is401WithErrorResponse() throws Exception {
        mvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.path").value("/api/orders"));
    }

    @Test
    void forgedToken_is401() throws Exception {
        String token = register("forged").get("accessToken").asString();
        // Flip one character of the signature: the payload is intact, the signature no longer matches.
        String forged = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");
        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired access token"));
    }

    @Test
    void customerCannotWriteCategories_is403WithErrorResponse() throws Exception {
        String token = register("cat").get("accessToken").asString();
        mvc.perform(auth(post("/api/categories"), token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void customerCannotListAllPayments_is403() throws Exception {
        String token = register("paylist").get("accessToken").asString();
        mvc.perform(auth(get("/api/payments/pay-list"), token)).andExpect(status().isForbidden());
    }

    @Test
    void catalogReadsStayPublic() throws Exception {
        mvc.perform(get("/api/products/" + productId)).andExpect(status().isOk());
    }

    // ---------- register / login ----------

    @Test
    void register_ignoresClientChosenRole() throws Exception {
        String body = """
                {"name":"Eve","email":"eve-%s@test.test","phone":"%s","password":"Password123!","role":"ADMIN"}
                """.formatted(tag, phone());
        String token = json.readTree(postJson("/api/auth/register", body)
                        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .get("accessToken").asString();
        assertEquals("CUSTOMER", jwtDecoder.decode(token).getClaimAsString("role"));
    }

    @Test
    void register_duplicateEmail_is409() throws Exception {
        register("dup");
        postJson("/api/auth/register", registerBody("dup", phone())).andExpect(status().isConflict());
    }

    @Test
    void login_wrongPasswordAndUnknownEmail_lookIdentical() throws Exception {
        register("login");
        String wrongPassword = postJson("/api/auth/login",
                "{\"email\":\"login-" + tag + "@test.test\",\"password\":\"wrong-password\"}")
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String unknownEmail = postJson("/api/auth/login",
                "{\"email\":\"nobody-" + tag + "@test.test\",\"password\":\"wrong-password\"}")
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertEquals(json.readTree(wrongPassword).get("message"), json.readTree(unknownEmail).get("message"));

        // Email is case-insensitive.
        postJson("/api/auth/login", "{\"email\":\"LOGIN-" + tag.toUpperCase() + "@TEST.TEST\",\"password\":\"Password123!\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    // ---------- customer id comes from the token; object-level authorization ----------

    @Test
    void ordersAndPayments_areScopedToTheTokensCustomer() throws Exception {
        String alice = register("alice").get("accessToken").asString();
        String bob = register("bob").get("accessToken").asString();

        // Alice orders. A "customerId" in the body is ignored (unknown field).
        String orderJson = """
                {"customerId":1,"storeId":%d,"items":[{"productId":%d,"quantity":1}]}""".formatted(storeId, productId);
        long aliceCustomerId = ((Number) jwtDecoder.decode(alice).getClaim("cid")).longValue();
        JsonNode order = json.readTree(mvc.perform(auth(post("/api/orders"), alice)
                        .contentType(MediaType.APPLICATION_JSON).content(orderJson))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long orderId = order.get("id").asLong();
        assertEquals(aliceCustomerId,
                jdbc.queryForObject("SELECT customer_id FROM orders WHERE id = ?", Long.class, orderId));

        // History: Alice sees it, Bob doesn't.
        mvc.perform(auth(get("/api/orders"), alice)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(auth(get("/api/orders"), bob)).andExpect(jsonPath("$.length()").value(0));

        // Bob can't pay or read Alice's order: 404, same as an order that doesn't exist.
        mvc.perform(auth(post("/api/payments"), bob).header("Idempotency-Key", "bob-" + tag)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"orderId\":" + orderId + "}"))
                .andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/payments").param("orderId", String.valueOf(orderId)), bob))
                .andExpect(status().isNotFound());

        // Alice pays.
        String aliceKey = "alice-" + tag;
        mvc.perform(auth(post("/api/payments"), alice).header("Idempotency-Key", aliceKey)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"orderId\":" + orderId + "}"))
                .andExpect(status().isCreated());

        // Bob reuses Alice's Idempotency-Key: must NOT get Alice's payment back.
        mvc.perform(auth(post("/api/payments"), bob).header("Idempotency-Key", aliceKey)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"orderId\":999999}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.orderId").doesNotExist());

        // Alice retrying with her own key still gets the replay.
        mvc.perform(auth(post("/api/payments"), alice).header("Idempotency-Key", aliceKey)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"orderId\":" + orderId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId));
    }

    // ---------- refresh rotation, reuse detection, logout ----------

    @Test
    void refresh_rotates_andReuseRevokesTheWholeSession() throws Exception {
        String r1 = register("rot").get("refreshToken").asString();

        String r2 = json.readTree(postJson("/api/auth/refresh", refreshBody(r1))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("refreshToken").asString();

        // r1 was rotated away. Presenting it again = reuse -> 401 AND the family (incl. r2) is revoked.
        postJson("/api/auth/refresh", refreshBody(r1)).andExpect(status().isUnauthorized());
        postJson("/api/auth/refresh", refreshBody(r2)).andExpect(status().isUnauthorized());
    }

    @Test
    void logout_endsTheSession() throws Exception {
        String r1 = register("out").get("refreshToken").asString();
        postJson("/api/auth/logout", refreshBody(r1)).andExpect(status().isNoContent());
        postJson("/api/auth/refresh", refreshBody(r1)).andExpect(status().isUnauthorized());
        // Idempotent: logging out twice is fine.
        postJson("/api/auth/logout", refreshBody(r1)).andExpect(status().isNoContent());
    }

    @Test
    void twentyConcurrentRefreshes_exactlyOneWins() throws Exception {
        String token = register("race").get("refreshToken").asString();
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);   // release all threads at once: maximum overlap
        AtomicInteger won = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Throwable> otherErrors = new CopyOnWriteArrayList<>();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                startGate.await();
                try {
                    refreshTokenService.rotate(token);
                    won.incrementAndGet();
                } catch (BadCredentialsException e) {
                    rejected.incrementAndGet();
                } catch (Throwable e) {
                    otherErrors.add(e);
                }
                return null;
            }));
        }
        startGate.countDown();
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(List.of(), otherErrors);
        assertEquals(1, won.get(), "exactly one refresh may succeed");
        assertEquals(threads - 1, rejected.get());
        // The losers presented an already-rotated token = reuse, so strict mode revokes the
        // whole family, including the winner's new token (documented trade-off, ADR 0001).
        Integer active = jdbc.queryForObject("""
                SELECT count(*) FROM refresh_tokens t JOIN users u ON u.id = t.user_id
                WHERE u.email = ? AND t.revoked_at IS NULL""", Integer.class, "race-" + tag + "@test.test");
        assertEquals(0, active);
    }

    // ---------- helpers ----------

    private JsonNode register(String who) throws Exception {
        return json.readTree(postJson("/api/auth/register", registerBody(who, phone()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private String registerBody(String who, String phone) {
        return """
                {"name":"%s","email":"%s-%s@test.test","phone":"%s","password":"Password123!"}
                """.formatted(who, who, tag, phone);
    }

    // Unique 12-digit phone per call (customers.phone is UNIQUE).
    private static String phone() {
        return String.valueOf(100_000_000_000L + ThreadLocalRandom.current().nextLong(899_999_999_999L));
    }

    private static String refreshBody(String token) {
        return "{\"refreshToken\":\"" + token + "\"}";
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, String token) {
        return req.header("Authorization", "Bearer " + token);
    }
}
