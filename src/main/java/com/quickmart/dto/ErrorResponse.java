package com.quickmart.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Every error the API returns has this shape, so the React Native app
 * can parse failures with one type instead of guessing per endpoint.
 *
 * fieldErrors is only filled for validation failures (field -> message).
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, Map.of());
    }
}
