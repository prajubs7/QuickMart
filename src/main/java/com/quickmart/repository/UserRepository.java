package com.quickmart.repository;

import com.quickmart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Callers pass an already lower-cased email (AuthService.normalize).
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
