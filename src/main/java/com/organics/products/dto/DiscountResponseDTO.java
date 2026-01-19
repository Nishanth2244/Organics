package com.organics.products.dto;

import com.organics.products.entity.DiscountScope;
import com.organics.products.entity.DiscountType;
import lombok.Data;

@Data
public class DiscountResponseDTO {

    private Long id;
    private String name;
    private DiscountType discountType;
    private Double discountValue;
    private DiscountScope scope;
    private Boolean active;
}
