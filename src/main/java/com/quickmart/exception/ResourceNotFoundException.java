package com.quickmart.exception;

/**
 * Services throw this when something doesn't exist (e.g. product 42).
 * The service only says "not found". The handler decides it becomes HTTP 404,
 * so the service layer stays free of web concerns.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
