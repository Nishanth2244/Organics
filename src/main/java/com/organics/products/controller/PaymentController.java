package com.organics.products.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.organics.products.dto.OrderResponse;
import com.organics.products.service.PaymentService;
import com.razorpay.RazorpayException;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/api")
public class PaymentController {
	
	@Autowired
	private PaymentService paymentService;
	
	@Value("${razor.key.id}")
	private String keyId;
	
	
    @PostMapping("/create-order")
	public OrderResponse placeOrder(@RequestParam Long cartId) throws RazorpayException {
		
		OrderResponse orderResponse = paymentService.createOrder(cartId);
		
		orderResponse.setKeyId(keyId);
		log.info("Order created succesfully orderId: {}", orderResponse.getRazorPayOrderId());
		return orderResponse;
	}

}
