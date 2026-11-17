package com.quickmart.exception;

/** Something with the same unique key already exists (or is still referenced), so HTTP 409 Conflict. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
