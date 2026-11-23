package com.quickmart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

// The cart arrives in the request body. A persisted cart table comes later if needed.
// No customerId: the customer is whoever the access token says (JwtClaims.customerId).
// A body field would let any caller place orders as any customer.
public record PlaceOrderRequest(
        @NotNull Long storeId,
        @NotEmpty List<@Valid Item> items   // @Valid on the element type: validate each item too
) {
    public record Item(
            @NotNull Long productId,
            @NotNull @Positive Integer quantity
    ) {
    }
}
