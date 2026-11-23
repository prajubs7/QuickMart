package com.quickmart.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor // public: ProductService builds it with setters (too many fields for a neat constructor)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price; // BigDecimal for money, matching NUMERIC(10,2)

    // LAZY: loading a product shouldn't also load its category unless we ask for it.
    // (@ManyToOne is EAGER by default, a JPA default you almost always want to override.)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private boolean active = true;

    // Owner (V11). null = platform catalog product, only ADMIN may change it.
    // No @Setter: set once at creation, ownership never moves through a PUT body.
    @Setter(AccessLevel.NONE)
    private Long vendorId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp // Hibernate sets this on every UPDATE, so no trigger needed
    @Column(nullable = false)
    private Instant updatedAt;

    public static Product ownedBy(Long vendorId) {
        Product p = new Product();
        p.vendorId = vendorId;
        return p;
    }
}
