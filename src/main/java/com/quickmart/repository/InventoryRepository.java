package com.quickmart.repository;

import com.quickmart.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    // Served by the unique index uq_inventory_store_product (store_id, product_id).
    Optional<Inventory> findByStoreIdAndProductId(Long storeId, Long productId);

    // "All stock in store S": the same unique index (store_id first) serves it.
    List<Inventory> findByStoreIdOrderByProductIdAsc(Long storeId);
}
