package com.quickmart.exception;

// 400 for rules that bean validation annotations can't express (e.g. password length in bytes).
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
