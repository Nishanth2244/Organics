package com.organics.products.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class CreateCouponRequest {

    private String code;
    private Double discountPercentage;
    private double minOrderAmount;
    private double maxDiscountAmount;
    private LocalDate expiryDate;
    private boolean active = true;
}
