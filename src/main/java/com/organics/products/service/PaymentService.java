package com.organics.products.service;

import java.time.LocalDate;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.organics.products.config.SecurityUtil;
import com.organics.products.dto.OrderResponse;
import com.organics.products.entity.Cart;
import com.organics.products.entity.OrderStatus;
import com.organics.products.entity.Payment;
import com.organics.products.entity.PaymentStatus;
import com.organics.products.exception.ResourceNotFoundException;
import com.organics.products.respository.CartRepository;
import com.organics.products.respository.OrderRepository;
import com.organics.products.respository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

import jakarta.transaction.Transactional;
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

	@Autowired
	private OrderRepository orderRepository;

	@Value("${razor.key.secret}")
	private String secretKey;
	
	
	@Autowired
	private OrderService orderService;

	@Transactional
	public OrderResponse createOrder(Long orderId) throws RazorpayException {

		Long userId = SecurityUtil.getCurrentUserId().orElseThrow(() -> new RuntimeException("User not authenticated"));

		com.organics.products.entity.Order ExistingOrder = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("orderId not found to do payment: " + orderId));

		if (!ExistingOrder.getUser().getId().equals(userId)) {
			throw new RuntimeException("Unauthorized order access");
		}

		Double amount = ExistingOrder.getOrderAmount();

		JSONObject orderRequest = new JSONObject();
		orderRequest.put("amount", amount * 100);
		orderRequest.put("currency", "INR");
		orderRequest.put("receipt", "ORDER_" + orderId);

		Order order = razorpayClient.orders.create(orderRequest);

		Payment payment = new Payment();
		payment.setRazorpayOrderId(order.get("id"));
		payment.setAmount(amount);
		payment.setPaymentStatus(PaymentStatus.PENDING);
		payment.setUser(ExistingOrder.getUser());
		payment.setOrder(ExistingOrder);
		payment.setPaymentDate(LocalDate.now());

		OrderResponse orderResponse = new OrderResponse();
		orderResponse.setAmount(amount);
		orderResponse.setRazorPayOrderId(order.get("id"));

		paymentRepository.save(payment);

		log.info("Payment details saved");
		return orderResponse;
	}

	@Transactional
	public boolean verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
		
		try {
			JSONObject verifyRequest = new JSONObject();
			verifyRequest.put("razorpay_order_id", razorpayOrderId);
			verifyRequest.put("razorpay_payment_id", razorpayPaymentId);
			verifyRequest.put("razorpay_signature", razorpaySignature);
			
			boolean isValid = Utils.verifyPaymentSignature(verifyRequest, secretKey);
			
			if (isValid) {
	            Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId);
	            
	            if(payment == null) {
	            	throw new ResourceNotFoundException("Payment record not found");
	            }
	            
	            com.razorpay.Payment razorpayPayment = razorpayClient.payments.fetch(razorpayPaymentId);
	            String method = razorpayPayment.get("method");
	            
	            payment.setRazorpayPaymentId(razorpayPaymentId);
	            payment.setRazorpaySignature(razorpaySignature);
	            payment.setMethod(method);
	            payment.setPaymentStatus(PaymentStatus.SUCCESSFUL);
	            payment.setPaymentDate(LocalDate.now());
	            paymentRepository.save(payment);
	            
	            com.organics.products.entity.Order order = payment.getOrder(); 
	            order.setPaymentStatus(PaymentStatus.SUCCESSFUL);
	            order.setOrderStatus(OrderStatus.CONFIRMED);
	            orderRepository.save(order);
	            
	            try {
	                orderService.sendOrderToShiprocket(order.getId());
	            } catch (Exception e) {
	                log.error("Payment success but Shiprocket failed: {}", e.getMessage());
	            }
	            
	            log.info("Payment verified and Order confirmed {}", order.getId());
	            return true;
			}
	
		}
		catch (Exception e) {
			log.error("Payment verification failed: {}", e.getMessage());
		}
		return false;
		
	}

}
