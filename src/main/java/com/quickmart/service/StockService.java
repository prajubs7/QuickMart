package com.quickmart.service;

import com.quickmart.config.Actor;
import com.quickmart.dto.StockResponse;
import com.quickmart.entity.Inventory;
import com.quickmart.entity.Product;
import com.quickmart.entity.Store;
import com.quickmart.exception.ResourceNotFoundException;
import com.quickmart.repository.InventoryRepository;
import com.quickmart.repository.ProductRepository;
import com.quickmart.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Vendor-facing stock management. Kept apart from InventoryService, which holds the
 * customer-side reserve() used while placing an order (the concurrency scenario).
 *
 * Ownership is decided by the STORE: a vendor manages stock only in stores whose vendor_id is theirs.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    // VENDOR (own store) or ADMIN.
    public List<StockResponse> list(Actor actor, Long storeId) {
        Store store = findStore(storeId);
        if (!actor.isAdmin() && !actor.isVendor(store.getVendorId())) {
            throw new AccessDeniedException("Store " + storeId + " belongs to another vendor");
        }
        return inventoryRepository.findByStoreIdOrderByProductIdAsc(storeId).stream()
                .map(StockResponse::from)
                .toList();
    }

    // VENDOR only, own store. Creates the inventory row the first time a product is stocked.
    //
    // Known gap: this writes an absolute number, so it can overwrite a reserve() that ran between
    // the vendor's shelf count and this write. The inventory @Version step (concurrency scenario)
    // closes that; until then the DB CHECK (quantity >= 0) is the only guard.
    @Transactional
    public StockResponse setStock(Actor actor, Long storeId, Long productId, int quantity) {
        Store store = findStore(storeId);
        if (!actor.isVendor(store.getVendorId())) {
            throw new AccessDeniedException("Store " + storeId + " belongs to another vendor");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " not found"));
        // Platform products (vendorId null) may be stocked by any store; a vendor's own product
        // only by that vendor. Otherwise vendor B could sell vendor A's private listing.
        if (product.getVendorId() != null && !actor.isVendor(product.getVendorId())) {
            throw new AccessDeniedException("Product " + productId + " belongs to another vendor");
        }

        Inventory stock = inventoryRepository.findByStoreIdAndProductId(storeId, productId).orElse(null);
        if (stock == null) {
            // Two concurrent first-time PUTs: the loser hits uq_inventory_store_product -> 409.
            stock = inventoryRepository.saveAndFlush(new Inventory(storeId, productId, quantity));
        } else {
            stock.setQuantity(quantity);
            inventoryRepository.flush(); // so updatedAt in the response is the new value
        }
        return StockResponse.from(stock);
    }

    // 404 for a store that doesn't exist, 403 (callers above) for one that isn't yours:
    // store ids are not secret (customers order from them), so there is nothing to hide.
    private Store findStore(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store " + storeId + " not found"));
    }
}
