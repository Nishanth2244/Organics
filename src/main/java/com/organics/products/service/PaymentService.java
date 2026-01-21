package com.organics.products.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.organics.products.dto.OrderResponse;
import com.organics.products.entity.Cart;
import com.organics.products.entity.Payment;
import com.organics.products.entity.PaymentStatus;
import com.organics.products.exception.ResourceNotFoundException;
import com.organics.products.respository.CartRepository;
import com.organics.products.respository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
public class PaymentService {
	
	@Autowired
	private CartRepository cartRepository;
	
	@Autowired
	private RazorpayClient razorpayClient;
	
	@Autowired
	private PaymentRepository paymentRepository;

	public OrderResponse createOrder(Long cartId) throws RazorpayException {
		
		Cart cart =  cartRepository.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart Id not found"));

		Double amount = cart.getPayableAmount();
		
		JSONObject orderRequest = new JSONObject();
		orderRequest.put("amount", amount*100);
		orderRequest.put("currency", "INR");
		orderRequest.put("receipt", String.valueOf(System.currentTimeMillis()));
		
		Order order = razorpayClient.orders.create(orderRequest);
		
		Payment payment = new Payment();
		payment.setRazorpayOrderId(order.get("id"));
		payment.setAmount(amount);
		payment.setPaymentStatus(PaymentStatus.PENDING);
		payment.setUser(cart.getUser());
		
		OrderResponse orderResponse = new OrderResponse();
		orderResponse.setAmount(amount);
		orderResponse.setOrderId(order.get("id"));
		
		paymentRepository.save(payment);
		
		log.info("Payment details saved");
		return orderResponse;
	}

}
