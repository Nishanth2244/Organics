package com.organics.products.dto;

import lombok.Data;

@Data
public class InventoryCreateRequest {

    private Long productId;
    private Long branchId;
    private Integer stock; // initial stock
}
