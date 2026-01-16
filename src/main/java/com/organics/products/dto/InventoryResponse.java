package com.organics.products.dto;

import lombok.Data;

@Data
public class InventoryResponse {

    private Long inventoryId;
    private Long productId;
    private Long branchId;

    private Integer availableStock;
    private Integer reservedStock;
}
