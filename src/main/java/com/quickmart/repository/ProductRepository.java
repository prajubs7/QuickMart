package com.quickmart.repository;

import com.quickmart.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Page = one page of rows + a COUNT(*) query for the total. Watch that count in the SQL log.
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    boolean existsBySku(String sku);

    boolean existsByCategoryId(Long categoryId);
}
