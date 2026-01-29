package com.organics.products.dto;

import com.organics.products.entity.DiscountType;
import lombok.Data;
import java.util.List;

import com.organics.products.entity.UnitType;

@Data
public class ProductDTO {
    private Long id;
    private String productName;
    private String brand;
    private String description;
    private Integer returnDays;
    private Double mrp;
    private Boolean status;
    private Long categoryId;
    private UnitType unit;
    private Double netWeight;
    private List<ImageDetailDTO> imageUrls;
    private Long inventoryId;
    private Integer availableStock;

    private Double finalPrice;
    private Double discountAmount;
    private DiscountType discountType;
}