package com.quickmart.service;

import com.quickmart.dto.PaymentRequest;
import com.quickmart.dto.PaymentResponse;
import com.quickmart.entity.Order;
import com.quickmart.entity.Payment;
import com.quickmart.exception.ResourceNotFoundException;
import com.quickmart.repository.OrderRepository;
import com.quickmart.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final MockPaymentGateway gateway;

    // Round A: check the key, then charge. A retry that arrives AFTER the first attempt saved
    // gets the stored payment back. A retry that arrives WHILE the first is still at the
    // gateway finds nothing and charges again -> that gap is what Round B closes.
    //
    // No @Transactional here: it would hold a DB connection for the whole slow gateway call.
    // With a pool of 10 connections, 10 slow payments would freeze every other endpoint.
    // findById and save each run in their own short transaction instead.
    public PaymentResponse pay(String idempotencyKey, PaymentRequest req) {
        // Seen this key before? Replay the original result: same request -> same answer, no new charge.
        // The client can't tell a replay from the first response, which is the point: a retry is safe.
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Replaying payment {} for idempotency key {}", existing.get().getId(), idempotencyKey);
            return PaymentResponse.from(existing.get());
        }

        Order order = orderRepository.findById(req.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order " + req.orderId() + " not found"));

        String gatewayRef = gateway.charge(order.getId(), order.getTotalAmount());

        // B1: same behaviour as Round A, just expressed with the new lifecycle.
        // B2 will move the PENDING insert to BEFORE gateway.charge(...).
        Payment payment = new Payment(order.getId(), order.getTotalAmount(), idempotencyKey);
        payment.markSucceeded(gatewayRef);
        payment = paymentRepository.save(payment);
        log.info("Saved payment {} for order {}", payment.getId(), order.getId());
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> forOrder(Long orderId) {
        return paymentRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
       return paymentRepository.findAll();
    }

}
    
