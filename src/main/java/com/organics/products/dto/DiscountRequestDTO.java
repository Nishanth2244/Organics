package com.organics.products.dto;

import com.organics.products.entity.DiscountScope;
import com.organics.products.entity.DiscountType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DiscountRequestDTO {

    private String name;
    private DiscountType discountType;
    private Double discountValue;
    private DiscountScope scope;
    private Boolean active;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Double minCartValue;
}
