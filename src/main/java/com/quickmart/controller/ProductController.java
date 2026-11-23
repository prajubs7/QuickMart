package com.quickmart.controller;

import com.quickmart.config.Actor;
import com.quickmart.dto.PageResponse;
import com.quickmart.dto.ProductRequest;
import com.quickmart.dto.ProductResponse;
import com.quickmart.service.CatalogCacheService;
import com.quickmart.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    // Reads go through the cache. Writes go straight to ProductService.
    private final CatalogCacheService catalogCache;

    // GET /api/products?categoryId=3&page=0&size=20&sort=price,desc
    @GetMapping
    public PageResponse<ProductResponse> list(@RequestParam(required = false) Long categoryId,
                                              @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return catalogCache.listProducts(categoryId, pageable);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return catalogCache.getProduct(id);
    }

    // Writes: VENDOR or ADMIN (SecurityConfig). WHICH products a vendor may touch is decided in
    // ProductService from the token's vendor id: a vendor edits only its own products.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProductRequest req) {
        return productService.create(Actor.from(jwt), req);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                  @Valid @RequestBody ProductRequest req) {
        return productService.update(Actor.from(jwt), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        productService.delete(Actor.from(jwt), id);
    }
}
