package com.organics.products.dto;

import lombok.Data;

@Data
public class OrderResponse {

    private String razorPayOrderId;
    private Double amount;
    private String keyId;

}
