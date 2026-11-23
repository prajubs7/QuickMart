package com.quickmart.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * "An order was placed." Published to Kafka; every interested service (notifications,
 * analytics, ...) reacts to it on its own. Past tense on purpose: it's a fact, not a command.
 *
 * eventId: unique per event. Nothing uses it yet. You'll need it soon.
 */
public record OrderPlacedEvent(
        UUID eventId,
        Long orderId,
        Long customerId,
        BigDecimal totalAmount,
        Instant occurredAt
) {
    public static final String TOPIC = "order-placed";
}
