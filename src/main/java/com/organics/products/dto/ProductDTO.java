package com.organics.products.dto;

import lombok.Data;
import java.util.List;

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
    private List<String> imageUrls; 
}