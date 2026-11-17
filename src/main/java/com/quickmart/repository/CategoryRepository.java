package com.quickmart.repository;

import com.quickmart.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Spring Data derives the SQL from the method name.
    boolean existsByNameIgnoreCase(String name);
}
