package com.organics.products.dto;

import com.organics.products.entity.UnitType;

import lombok.Data;


@Data
public class CartItemDTO {

    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Double mrp;
    private Double discountPercent;
    private Double itemTotalMrp;
    private Double discountAmount;
    private Double finalPrice;
    private String imageUrl;
    private UnitType unit;
    private Double netWeight;
}