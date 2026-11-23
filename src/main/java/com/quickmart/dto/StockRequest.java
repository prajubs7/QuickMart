package com.quickmart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Absolute quantity, not a delta: PUT with the same body twice leaves the same stock (idempotent).
public record StockRequest(@NotNull @Min(0) @Max(1_000_000) Integer quantity) {
}
