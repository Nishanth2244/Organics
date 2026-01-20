package com.organics.products.dto;

import com.organics.products.entity.OrderStatus;
import lombok.Data;

import java.util.List;

@Data
public class OrderResponseDTO {
    private Long orderId;
    private Double orderAmount;
    private OrderStatus status;
    private List<OrderItemDTO> items;
}
