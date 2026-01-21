// OrderRequestDTO.java
package com.organics.products.dto;

import lombok.Data;

@Data
public class OrderRequestDTO {
    private String description;
    private Long addressId; // If you have address management
    private String paymentMethod; // e.g., "COD", "ONLINE", etc.
}