// OrderItemDTO.java
package com.organics.products.dto;

import lombok.Data;

@Data
public class OrderItemDTO {
    private Long productId;
    private String productName;
    private Integer quantity;
    private Double price;
    private Double tax;
    private Double discount;
    private Double totalPrice;
    private String imageUrl;
}