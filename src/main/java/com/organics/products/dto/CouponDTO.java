package com.organics.products.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CouponDTO {
    private Long id;
    private String code;
    private Double discountPercentage;
    private Double maxDiscountAmount;
    private Double minOrderAmount;
    private LocalDate expiryDate;
    private boolean isActive;
}