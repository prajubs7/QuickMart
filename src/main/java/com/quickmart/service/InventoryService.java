package com.quickmart.service;

import com.quickmart.entity.Inventory;
import com.quickmart.exception.InsufficientStockException;
import com.quickmart.exception.ResourceNotFoundException;
import com.quickmart.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    /**
     * NAIVE read-then-write. Broken under concurrency on purpose (scenario 6).
     *
     * 1. SELECT quantity        -> every concurrent buyer can read the same "10"
     * 2. check in Java          -> they all pass the check
     * 3. UPDATE quantity = 9    -> they all write the value THEY computed; the last write wins
     *
     * Nothing between step 1 and step 3 stops another transaction from doing the same.
     */
    @Transactional
    public void reserve(Long storeId, Long productId, int quantity) {
        Inventory stock = inventoryRepository.findByStoreIdAndProductId(storeId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product " + productId + " is not sold at store " + storeId));

        if (stock.getQuantity() < quantity) {
            throw new InsufficientStockException("Only " + stock.getQuantity() + " left of product "
                    + productId + ", requested " + quantity);
        }
        stock.setQuantity(stock.getQuantity() - quantity);
        // No save() needed: `stock` is managed, so Hibernate flushes the UPDATE at commit (dirty checking).
    }
}
