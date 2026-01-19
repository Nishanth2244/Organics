package com.organics.products.dto;

import lombok.Data;

@Data
public class AddToCartRequest {

	private Long inventoryId;     
    private int quantity;
}
