package com.quickmart.dto;

import com.quickmart.entity.Product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        Long categoryId,
        boolean active,
        Long vendorId   // null = platform catalog product
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getPrice(),
                // getId() on a LAZY proxy returns the FK value without querying categories.
                // Calling p.getCategory().getName() here instead would fire one extra query per product.
                p.getCategory().getId(),
                p.isActive(),
                p.getVendorId());
    }
}
