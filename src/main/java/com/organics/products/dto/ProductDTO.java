
package com.organics.products.dto;

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
    private Double afterDiscount;
    private Double discount;
    private Boolean status;
    private Long categoryId;
    private UnitType unit;
    private Double netWeight;
    private List<String> imageUrls; 
}
