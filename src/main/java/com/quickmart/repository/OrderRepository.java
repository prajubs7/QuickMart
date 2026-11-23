package com.quickmart.repository;

import com.quickmart.entity.Order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph (attributePaths = {"items", "items.product"})
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    // Object-level authorization in the query itself: "order X, IF it belongs to customer Y".
    // Someone else's order and a non-existent order look identical to the caller (both empty -> 404).
    Optional<Order> findByIdAndCustomerId(Long id, Long customerId);

    boolean existsByIdAndCustomerId(Long id, Long customerId);

}
