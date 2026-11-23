package com.quickmart.service;

import com.quickmart.dto.OrderPlacedEvent;
import com.quickmart.dto.OrderResponse;
import com.quickmart.dto.PlaceOrderRequest;
import com.quickmart.entity.Order;
import com.quickmart.entity.Product;
import com.quickmart.exception.ResourceNotFoundException;
import com.quickmart.repository.OrderRepository;
import com.quickmart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final ProductRepository productRepository;
    // Key = orderId (String), value = the event. Boot builds it from spring.kafka.producer.*.
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    // Not final, so @RequiredArgsConstructor skips it and Spring injects it as a field.
    @Value("${quickmart.email.delay-ms}")
    private long emailDelayMs;

    // Entry point used by the controller.
    @Transactional
    // customerId comes from the access token (OrderController), never from the request body.
    public OrderResponse placeOrder(Long customerId, PlaceOrderRequest req) {
        log.info("Placing order for customer {} at store {} ({} items)",
                customerId, req.storeId(), req.items().size());

        Order order = new Order(customerId, req.storeId());

        for (PlaceOrderRequest.Item line : req.items()) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product " + line.productId() + " not found"));

            // Joins this transaction (default REQUIRED), so a later failure still rolls the stock back.
            inventoryService.reserve(req.storeId(), line.productId(), line.quantity());

            order.addItem(product, line.quantity());
        }

        orderRepository.save(order); // cascade inserts the order_items too

        // BROKEN ON PURPOSE (dual write): we write to Postgres AND Kafka, and nothing ties them together.
        OrderPlacedEvent event = new OrderPlacedEvent(UUID.randomUUID(), order.getId(),
                order.getCustomerId(), order.getTotalAmount(), Instant.now());
        kafkaTemplate.send(OrderPlacedEvent.TOPIC, order.getId().toString(), event);

        // BROKEN ON PURPOSE (self-invocation): the email should go out in the background.
        sendConfirmationEmail(order.getId(), order.getCustomerId());

        return OrderResponse.from(order);
    }

    // Mock SMTP call. Should run on an "email-N" thread from AsyncConfig.emailExecutor.
    @Async("emailExecutor")
    public void sendConfirmationEmail(Long orderId, Long customerId) {
        log.info("Email: sending confirmation for order {} to customer {}", orderId, customerId);
        try {
            Thread.sleep(emailDelayMs); // SMTP server being slow
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        log.info("Email: sent for order {}", orderId);
    }

    // Order history: every order of a customer, with its items and product names.
    @Transactional(readOnly = true)
    public List<OrderResponse> history(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(OrderResponse::from)
                .toList();
    }
}
