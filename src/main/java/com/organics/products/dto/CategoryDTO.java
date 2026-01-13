package com.organics.products.dto;

import lombok.Data;

@Data
public class CategoryDTO {
    private Long id;
    private String categoryName;
    private String description;
    private String categoryImage;
    private Boolean status;
}