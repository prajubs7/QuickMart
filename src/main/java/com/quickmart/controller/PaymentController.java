package com.quickmart.controller;

import com.quickmart.dto.PaymentRequest;
import com.quickmart.dto.PaymentResponse;
import com.quickmart.entity.Payment;
import com.quickmart.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) 
    public PaymentResponse pay(@RequestHeader("Idempotency-Key") String idempotencyKey,
                               @Valid @RequestBody PaymentRequest req) {
        return paymentService.pay(idempotencyKey, req);
    }

    // GET /api/payments?orderId=1 -> how many times was this order charged?
    @GetMapping
    public List<PaymentResponse> forOrder(@RequestParam Long orderId) {
        return paymentService.forOrder(orderId);
    }

    @GetMapping("/pay-list")
    public List<Payment> PaymentList() {
        return paymentService.getAllPayments();
    }
}
