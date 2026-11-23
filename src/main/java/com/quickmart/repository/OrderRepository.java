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

    // Same ownership filter, but loads items + products in one query for the order detail response.
    // ("WithItems" is ignored by Spring Data; it only names the method apart from the one above,
    // which PaymentService uses and which must NOT drag the items along.)
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findWithItemsByIdAndCustomerId(Long id, Long customerId);

    // ADMIN read: no owner filter. Only reachable through /api/admin/** (SecurityConfig).
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findWithItemsById(Long id);

}
