package com.quickmart.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plain FK values, not @ManyToOne: there is no Store entity yet, and reserving stock
    // only needs the numbers. No relation means no lazy proxy to trip over.
    @Column(nullable = false)
    private Long storeId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    // The `version` column exists in the table but is NOT mapped yet (concurrency step).
    // ddl-auto=validate only checks that mapped fields have columns, not the other way round.

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    // Created when a vendor stocks a product in a store for the first time (StockService.setStock).
    public Inventory(Long storeId, Long productId, int quantity) {
        this.storeId = storeId;
        this.productId = productId;
        this.quantity = quantity;
    }
}
