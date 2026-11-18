package com.quickmart.exception;

/** The external payment provider failed. Not our bug, so HTTP 502 Bad Gateway. */
public class PaymentGatewayException extends RuntimeException {
    public PaymentGatewayException(String message) {
        super(message);
    }
}
