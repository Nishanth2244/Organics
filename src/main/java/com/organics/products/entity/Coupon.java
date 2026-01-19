package com.organics.products.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Data
@Table(name = "coupons")
public class Coupon {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    private String description;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType; 

    private Double discountValue;

    private Double minOrderAmount;
    private Double maxDiscountAmount; 

    private Integer usageLimit; 
    private Integer usedCount;

    private LocalDate startDate;
    private LocalDate endDate;

    private boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "coupon")
    private List<CartCoupon> cartCoupons = new ArrayList<>();

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
