package com.organics.products.dto;

import lombok.Data;
import java.time.LocalDate;

import com.organics.products.entity.DiscountType;

@Data
public class CouponDTO {
    private Long id;
    private String code;
    private String description;
    private DiscountType discountType;
    private Double discountValue;
    private Double minOrderAmount;
    private Double maxDiscountAmount;
    private Integer usageLimit;
    private Integer usedCount;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isActive;
}