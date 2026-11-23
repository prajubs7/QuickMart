package com.quickmart.controller;

import com.quickmart.config.JwtClaims;
import com.quickmart.dto.PaymentRequest;
import com.quickmart.dto.PaymentResponse;
import com.quickmart.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // CUSTOMER only. You can only pay for your own order.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse pay(@AuthenticationPrincipal Jwt jwt,
                               @RequestHeader("Idempotency-Key") String idempotencyKey,
                               @Valid @RequestBody PaymentRequest req) {
        return paymentService.pay(JwtClaims.customerId(jwt), idempotencyKey, req);
    }

    // CUSTOMER only. GET /api/payments?orderId=1 -> payments of MY order 1 (404 if it isn't mine).
    @GetMapping
    public List<PaymentResponse> forOrder(@AuthenticationPrincipal Jwt jwt, @RequestParam Long orderId) {
        return paymentService.forOrder(JwtClaims.customerId(jwt), orderId);
    }

    // ADMIN only (SecurityConfig). Returns DTOs, not entities.
    @GetMapping("/pay-list")
    public List<PaymentResponse> all() {
        return paymentService.getAllPayments();
    }
}
