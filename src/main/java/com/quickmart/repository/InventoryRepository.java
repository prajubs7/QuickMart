package com.quickmart.repository;

import com.quickmart.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    // Served by the unique index uq_inventory_store_product (store_id, product_id).
    Optional<Inventory> findByStoreIdAndProductId(Long storeId, Long productId);
}
