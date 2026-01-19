package com.organics.products.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "discounts")
@Data
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    private Double discountValue;

    @Enumerated(EnumType.STRING)
    private DiscountScope scope;

    private Boolean active;

    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    // For CART discount minimum cart value
    private Double minCartValue;
}
