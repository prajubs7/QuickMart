package com.quickmart.service;

import com.quickmart.dto.PaymentRequest;
import com.quickmart.dto.PaymentResponse;
import com.quickmart.entity.Order;
import com.quickmart.entity.Payment;
import com.quickmart.exception.DuplicateResourceException;
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
    // customerId comes from the access token (PaymentController).
    public PaymentResponse pay(Long customerId, String idempotencyKey, PaymentRequest req) {
        // Seen this key before? Replay the original result: same request -> same answer, no new charge.
        // The client can't tell a replay from the first response, which is the point: a retry is safe.
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Payment payment = existing.get();
            // Keys are unique across ALL customers (V7). Without this check, anyone who sends another
            // customer's key gets that customer's payment back. Refuse without saying whose it is.
            if (!orderRepository.existsByIdAndCustomerId(payment.getOrderId(), customerId)) {
                log.warn("Customer {} sent an Idempotency-Key that belongs to another customer", customerId);
                throw new DuplicateResourceException("Idempotency-Key has already been used");
            }
            log.info("Replaying payment {} for idempotency key {}", payment.getId(), idempotencyKey);
            return PaymentResponse.from(payment);
        }

        // Ownership is part of the lookup. Someone else's order -> 404, same as a missing one,
        // so the caller can't probe which order ids exist (403 would confirm "it exists, not yours").
        Order order = orderRepository.findByIdAndCustomerId(req.orderId(), customerId)
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
    public List<PaymentResponse> forOrder(Long customerId, Long orderId) {
        if (!orderRepository.existsByIdAndCustomerId(orderId, customerId)) {
            throw new ResourceNotFoundException("Order " + orderId + " not found");
        }
        return paymentRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    // ADMIN only. Still unbounded: becomes a paginated export in the large-data step.
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(PaymentResponse::from)
                .toList();
    }

}
    
