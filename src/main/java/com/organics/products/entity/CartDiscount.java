package com.organics.products.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "cart_discounts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id"}))
@Data
public class CartDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long cartId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id", nullable = false)
    private Discount discount;

}
