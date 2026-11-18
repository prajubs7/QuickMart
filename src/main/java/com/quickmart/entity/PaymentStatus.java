package com.quickmart.entity;

// Lifecycle: PENDING -> SUCCESS or PENDING -> FAILED. Nothing else.
// Stored as text (@Enumerated(STRING)), so adding values anywhere in this list is safe.
public enum PaymentStatus {
    PENDING,   // key reserved, gateway call in progress (or the app crashed mid-call)
    SUCCESS,   // gateway charged the customer; gatewayRef is set
    FAILED     // gateway refused or errored; the customer was not charged
}
