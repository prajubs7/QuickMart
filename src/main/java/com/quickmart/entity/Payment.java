package com.quickmart.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plain FK value, same reason as Order.customerId: we only need the number.
    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    // Null while PENDING (V8 dropped NOT NULL). Set once the gateway confirms the charge.
    @Column(length = 64)
    private String gatewayRef;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(length = 100)   // no nullable=false: old rows have NULL
    private String idempotencyKey;

    // Every payment starts PENDING with no gatewayRef: the row exists before the charge does.
    public Payment(Long orderId, BigDecimal amount, String idempotencyKey) {
        this.orderId = orderId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.status = PaymentStatus.PENDING;
    }

    // Methods instead of @Setter: only PENDING -> SUCCESS/FAILED is legal.
    // A setter would happily allow SUCCESS -> PENDING and corrupt the payment history.
    public void markSucceeded(String gatewayRef) {
        requirePending();
        this.gatewayRef = gatewayRef;
        this.status = PaymentStatus.SUCCESS;
    }

    public void markFailed() {
        requirePending();
        this.status = PaymentStatus.FAILED;
    }

    private void requirePending() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment " + id + " is already " + status);
        }
    }
}
