package com.organics.products.dto;

import lombok.Data;

@Data
public class OrderResponse {

    private String orderId;
    private Double amount;
    private String keyId;

}
