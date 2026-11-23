package com.quickmart.repository;

import com.quickmart.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    // Served by idx_addresses_customer_id.
    List<Address> findByCustomerIdOrderByIdAsc(Long customerId);

    // Ownership inside the query, same pattern as OrderRepository.findByIdAndCustomerId:
    // someone else's address and a missing one both come back empty -> 404.
    Optional<Address> findByIdAndCustomerId(Long id, Long customerId);

    // Clears the old default before a new one is inserted. The partial unique index
    // uq_addresses_one_default still catches two concurrent "make me default" requests (-> 409).
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Address a SET a.defaultAddress = false WHERE a.customerId = :customerId AND a.defaultAddress = true")
    int clearDefault(Long customerId);
}
