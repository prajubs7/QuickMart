package com.quickmart.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plain FK values: no Customer/Store entities yet. The DB foreign keys still guarantee they exist.
    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Long storeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // mappedBy: order_items.order_id is owned by OrderItem.order, so Order doesn't write it.
    // cascade ALL: saving the order also inserts its items.
    // @OneToMany is LAZY by default: the items are NOT loaded with the order.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    public Order(Long customerId, Long storeId) {
        this.customerId = customerId;
        this.storeId = storeId;
        this.status = OrderStatus.PLACED;
    }

    // The only way to add items, so both sides of the relation and the total stay in sync.
    public void addItem(Product product, int quantity) {
        OrderItem item = new OrderItem(this, product, quantity, product.getPrice());
        items.add(item);
        totalAmount = totalAmount.add(item.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
    }
}
