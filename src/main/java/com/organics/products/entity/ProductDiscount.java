package com.organics.products.entity;

import jakarta.persistence.*;
import lombok.Data;
@Entity
@Table(name = "product_discounts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id"}))
@Data
public class ProductDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id", nullable = false)
    private Discount discount;
}
