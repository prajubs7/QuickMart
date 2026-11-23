package com.quickmart.controller;

import com.quickmart.config.JwtClaims;
import com.quickmart.dto.OrderResponse;
import com.quickmart.dto.PlaceOrderRequest;
import com.quickmart.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// CUSTOMER only (SecurityConfig). The customer id always comes from the verified token.
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlaceOrderRequest req) {
        return orderService.placeOrder(JwtClaims.customerId(jwt), req);
    }

    // GET /api/orders -> MY orders. There is no ?customerId= any more: you can only see your own.
    @GetMapping
    public List<OrderResponse> history(@AuthenticationPrincipal Jwt jwt) {
        return orderService.history(JwtClaims.customerId(jwt));
    }

    // GET /api/orders/{id} -> one of MY orders. Another customer's id gives 404, same as a missing one.
    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return orderService.get(JwtClaims.customerId(jwt), id);
    }
}
