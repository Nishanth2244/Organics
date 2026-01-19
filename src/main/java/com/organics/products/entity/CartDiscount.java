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

    @ManyToOne
    @JoinColumn(name = "discount_id")
    private Discount discount;
}
