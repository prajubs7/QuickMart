package com.quickmart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

// The cart arrives in the request body. A persisted cart table comes later if needed.
public record PlaceOrderRequest(
        @NotNull Long customerId,
        @NotNull Long storeId,
        @NotEmpty List<@Valid Item> items   // @Valid on the element type: validate each item too
) {
    public record Item(
            @NotNull Long productId,
            @NotNull @Positive Integer quantity
    ) {
    }
}
