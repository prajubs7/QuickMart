package com.quickmart.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Read-only mapping of the V2 stores table. Stores are created by SQL for now;
// the app only needs "who owns store S?" for vendor authorization.
@Entity
@org.hibernate.annotations.Immutable // never written by the app: Hibernate skips dirty checking
@Table(name = "stores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long vendorId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private boolean active;
}
