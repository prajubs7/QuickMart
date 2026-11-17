package com.quickmart.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

// Used for both create (POST) and full replace (PUT).
public record ProductRequest(
        @NotBlank @Size(max = 50) String sku,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal price, // fits NUMERIC(10,2)
        @NotNull Long categoryId,
        Boolean active   // optional: null means "true" on create, "unchanged" on update
) {
}
