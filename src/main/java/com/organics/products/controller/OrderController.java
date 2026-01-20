package com.organics.products.controller;

import com.organics.products.dto.OrderResponseDTO;
import com.organics.products.dto.PlaceOrderRequestDTO;
import com.organics.products.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/place")
    public OrderResponseDTO placeOrder(@RequestBody PlaceOrderRequestDTO dto) {
        return orderService.placeOrder(dto);
    }
}
