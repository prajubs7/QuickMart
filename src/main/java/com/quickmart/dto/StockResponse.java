package com.quickmart.dto;

import com.quickmart.entity.Inventory;

import java.time.Instant;

public record StockResponse(Long storeId, Long productId, int quantity, Instant updatedAt) {

    public static StockResponse from(Inventory i) {
        return new StockResponse(i.getStoreId(), i.getProductId(), i.getQuantity(), i.getUpdatedAt());
    }
}
