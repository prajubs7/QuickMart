package com.quickmart.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Why not Lombok @Data on entities?
 *  - its equals/hashCode use every field. Change a field and the entity "disappears" from a HashSet,
 *    and touching a lazy relation inside equals() triggers surprise queries.
 *  - its toString() follows relations, which means lazy-loading or infinite recursion.
 * @Getter/@Setter give us the boilerplate without those traps.
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA needs a no-arg constructor; nobody else should use it
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // matches GENERATED ALWAYS AS IDENTITY
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Category(String name) {
        this.name = name;
    }
}
