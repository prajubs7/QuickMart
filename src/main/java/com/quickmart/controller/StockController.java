package com.quickmart.controller;

import com.quickmart.config.Actor;
import com.quickmart.dto.StockRequest;
import com.quickmart.dto.StockResponse;
import com.quickmart.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Stock of one store. SecurityConfig checks the ROLE; StockService checks that the store is the caller's.
@RestController
@RequestMapping("/api/stores/{storeId}/inventory")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    // VENDOR (own store) or ADMIN.
    @GetMapping
    public List<StockResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable Long storeId) {
        return stockService.list(Actor.from(jwt), storeId);
    }

    // VENDOR only, own store. PUT /api/stores/3/inventory/7 {"quantity": 40} -> store 3 now has 40 of product 7.
    @PutMapping("/{productId}")
    public StockResponse set(@AuthenticationPrincipal Jwt jwt, @PathVariable Long storeId,
                             @PathVariable Long productId, @Valid @RequestBody StockRequest req) {
        return stockService.setStock(Actor.from(jwt), storeId, productId, req.quantity());
    }
}
