package com.quickmart.repository;

import com.quickmart.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderIdOrderByCreatedAtAsc(Long orderId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

}
