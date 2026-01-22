package com.organics.products.dto;


import lombok.Data;

@Data
public class TopOrderedProductsDTO {
    private Long productId;
    private String productName;
    private Long totalQuantityOrdered;
    private Long orderCount;
    private Double totalRevenue;
    
    // Constructor for repository query
    public TopOrderedProductsDTO(Long productId, String productName, Long totalQuantityOrdered, Long orderCount, Double totalRevenue) {
        this.productId = productId;
        this.productName = productName;
        this.totalQuantityOrdered = totalQuantityOrdered;
        this.orderCount = orderCount;
        this.totalRevenue = totalRevenue;
    }
}
