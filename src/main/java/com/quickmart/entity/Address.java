package com.quickmart.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The owner. Plain FK value, same style as Order.customerId. Always taken from the token.
    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false, length = 50)
    private String label;

    @Column(nullable = false)
    private String line1;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 10)
    private String pincode;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Address(Long customerId, String label, String line1, String city, String pincode, boolean defaultAddress) {
        this.customerId = customerId;
        this.label = label;
        this.line1 = line1;
        this.city = city;
        this.pincode = pincode;
        this.defaultAddress = defaultAddress;
    }
}
