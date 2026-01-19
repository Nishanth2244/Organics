package com.organics.products.dto;

import com.organics.products.entity.UnitType;

import lombok.Data;


@Data
public class CartItemDTO {

    private Long id;
    private Long productId;
    private Long inventoryId;
    private String productName;
    private Integer quantity;
    private Double mrp;
    private String imageUrl;
    private UnitType unit;
    private Double netWeight;
}