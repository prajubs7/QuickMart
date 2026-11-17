package com.quickmart.service;

import com.quickmart.dto.OrderResponse;
import com.quickmart.dto.PlaceOrderRequest;
import com.quickmart.entity.Inventory;
import com.quickmart.entity.Order;
import com.quickmart.entity.Product;
import com.quickmart.exception.InsufficientStockException;
import com.quickmart.exception.ResourceNotFoundException;
import com.quickmart.repository.InventoryRepository;
import com.quickmart.repository.OrderRepository;
import com.quickmart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    // Entry point used by the controller.
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest req) {
        log.info("Placing order for customer {} at store {} ({} items)",
                req.customerId(), req.storeId(), req.items().size());

         Order order = new Order(req.customerId(), req.storeId());

        for (PlaceOrderRequest.Item line : req.items()) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product " + line.productId() + " not found"));

            Inventory stock = inventoryRepository.findByStoreIdAndProductId(req.storeId(), line.productId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product " + line.productId() + " is not sold at store " + req.storeId()));

            if (stock.getQuantity() < line.quantity()) {
                throw new InsufficientStockException("Only " + stock.getQuantity() + " left of product "
                        + line.productId() + ", requested " + line.quantity());
            }
            stock.setQuantity(stock.getQuantity() - line.quantity());
            inventoryRepository.save(stock);

            order.addItem(product, line.quantity());
        }

        orderRepository.save(order); // cascade inserts the order_items too
        return OrderResponse.from(order);
        
    }


    // Order history: every order of a customer, with its items and product names.
    @Transactional(readOnly = true)
    public List<OrderResponse> history(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(OrderResponse::from)
                .toList();
    }
}
