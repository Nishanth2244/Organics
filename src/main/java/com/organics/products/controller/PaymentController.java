package com.organics.products.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
	public OrderResponse placeOrder(@RequestParam Long orderId) throws RazorpayException {
		
		OrderResponse orderResponse = paymentService.createOrder(orderId);
		
		orderResponse.setKeyId(keyId);
		log.info("Order created succesfully orderId: {}", orderResponse.getRazorPayOrderId());
		return orderResponse;
	}
    
    
    
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> data) {
        String orderId = data.get("razorPayOrderId");
        String paymentId = data.get("razorPayPaymentId");
        String signature = data.get("razorPaySignature");
        
        log.info("Razorpay signature: {}", signature);

        boolean isVerified = paymentService.verifyPayment(orderId, paymentId, signature);

        if (isVerified) {
            return ResponseEntity.ok(Map.of("status", "success", "message", "Payment Verified"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "failed", "message", "Invalid Signature"));
        }
    }

}
