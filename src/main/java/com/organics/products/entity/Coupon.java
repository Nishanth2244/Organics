package com.organics.products.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    private Double discountPercentage;
    private Double maxDiscountAmount;
    private Double minOrderAmount;
    private LocalDate expiryDate;
    private boolean isActive = true;
}