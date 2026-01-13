package com.organics.products.dto;

import lombok.Data;
import java.util.List;

@Data
public class CartDTO {
    private Long id;
    private Double totalAmount;
    private boolean isActive;
    private List<CartItemDTO> items;
    private Long customerId;
}