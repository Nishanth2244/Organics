package com.organics.products.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "category_discounts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"category_id"}))
@Data
public class CategoryDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "discount_id")
    private Discount discount;
}
