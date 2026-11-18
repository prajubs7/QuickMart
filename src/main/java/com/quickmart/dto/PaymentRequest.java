package com.quickmart.dto;

import jakarta.validation.constraints.NotNull;

// No amount field on purpose: the server charges the order's own total.
// Trusting a client-sent amount would let anyone pay 1 rupee for a 5000 rupee order.
public record PaymentRequest(@NotNull Long orderId) {
}
