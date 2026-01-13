package com.organics.products.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProductDTO {
    private Long id;
    private String productName;
    private String brand;
    private String description;
    private Double discount;
    private Integer returnDays;
    private Double mrp;
    private Boolean status;
    private Long categoryId;
    private List<String> imageUrls; 
}