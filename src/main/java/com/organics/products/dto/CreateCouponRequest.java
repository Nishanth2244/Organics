package com.organics.products.dto;

import java.time.LocalDate;

import com.organics.products.entity.DiscountType;

import lombok.Data;

@Data
public class CreateCouponRequest {
    private String code;
    private String description;
    private DiscountType discountType;
    private Double discountValue;
    private Double minOrderAmount;
    private Double maxDiscountAmount;
    private Integer usageLimit;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active = true;
}