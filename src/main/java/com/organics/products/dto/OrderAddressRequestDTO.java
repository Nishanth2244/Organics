// OrderAddressRequestDTO.java
package com.organics.products.dto;

import lombok.Data;

@Data
public class OrderAddressRequestDTO {
    private String description;
    private String paymentMethod;
    private Long addressId; // ID of the selected address from user's addresses
}