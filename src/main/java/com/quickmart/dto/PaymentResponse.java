package com.quickmart.dto;

import com.quickmart.entity.Payment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(Long id, Long orderId, BigDecimal amount, String status,
                              String gatewayRef, Instant createdAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getAmount(), p.getStatus().name(),
                p.getGatewayRef(), p.getCreatedAt());
    }
}
