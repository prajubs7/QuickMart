package com.quickmart.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "users")   // "user" is a reserved word in Postgres
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // Plain FK values, same style as Order.customerId.
    private Long customerId;
    private Long vendorId;

    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // The only public way to create a user. There is deliberately no factory for VENDOR/ADMIN:
    // those come from seed scripts, never from a public endpoint.
    public static User customer(String email, String passwordHash, Long customerId) {
        User u = new User();
        u.email = email;
        u.passwordHash = passwordHash;
        u.role = Role.CUSTOMER;
        u.customerId = customerId;
        return u;
    }
}
