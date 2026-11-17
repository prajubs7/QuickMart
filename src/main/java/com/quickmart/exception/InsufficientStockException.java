package com.quickmart.exception;

/**
 * Thrown when a store doesn't have enough units to fill an order line.
 *
 * Unchecked on purpose: Spring's @Transactional rolls back only on RuntimeException/Error by default.
 * As a checked exception it was treated as a "normal" outcome, the transaction COMMITTED,
 * and stock already reserved for earlier lines leaked.
 */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
