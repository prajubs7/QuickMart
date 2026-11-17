package com.quickmart.dto;

import com.quickmart.entity.Order;
import com.quickmart.entity.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long customerId,
        Long storeId,
        String status,
        BigDecimal totalAmount,
        Instant createdAt,
        List<Item> items
) {
    public record Item(Long productId, String productName, int quantity, BigDecimal unitPrice) {

        static Item from(OrderItem i) {
            // getName() needs the real product row, so it loads the lazy product proxy.
            return new Item(i.getProduct().getId(), i.getProduct().getName(), i.getQuantity(), i.getUnitPrice());
        }
    }

    public static OrderResponse from(Order o) {
        return new OrderResponse(
                o.getId(), o.getCustomerId(), o.getStoreId(), o.getStatus().name(),
                o.getTotalAmount(), o.getCreatedAt(),
                o.getItems().stream().map(Item::from).toList()); // touching the lazy items collection
    }
}
