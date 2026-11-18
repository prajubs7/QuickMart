package com.quickmart.service;

import com.quickmart.exception.PaymentGatewayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stands in for Razorpay/Stripe. Behaves like a real third party on a bad day:
 * it is slow (delay-ms) and sometimes fails (failure-rate).
 * Both come from application.yml, so you can change them without touching code.
 */
@Slf4j
@Component
public class MockPaymentGateway {

    private final long delayMs;
    private final double failureRate;

    public MockPaymentGateway(@Value("${quickmart.payment-gateway.delay-ms}") long delayMs,
                              @Value("${quickmart.payment-gateway.failure-rate}") double failureRate) {
        this.delayMs = delayMs;
        this.failureRate = failureRate;
    }

    /** Charges the customer and returns the gateway's reference for the charge. */
    public String charge(Long orderId, BigDecimal amount) {
        log.info("Gateway: charging order {} amount {} (delay {} ms)", orderId, amount, delayMs);
        sleep(delayMs); // network round trip + the bank taking its time

        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            log.warn("Gateway: charge for order {} FAILED", orderId);
            throw new PaymentGatewayException("Payment gateway error");
        }

        String ref = "pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("Gateway: charged order {} -> {}", orderId, ref);
        return ref;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // keep the flag set for whoever interrupted us
            throw new PaymentGatewayException("Payment gateway call interrupted");
        }
    }
}
