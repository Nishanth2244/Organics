package com.organics.products.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.organics.products.dto.AddToCartRequest;
import com.organics.products.dto.CartDTO;
import com.organics.products.service.CartService;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/api/cart")
public class CartController {
	
	@Autowired
	private CartService cartService;
	
	
	@PostMapping("/addItem")
	public CartDTO addCart(@RequestBody AddToCartRequest addToCartRequest) {
		
		CartDTO cartDTO = cartService.addToCart(addToCartRequest);
		
		log.info("Item added to cart Succesfully: {}");
		return cartDTO;
	}
	
	
	@GetMapping("/myCart")
	public CartDTO getCart() {
		
		CartDTO cartDTO = cartService.myCart();
		return cartDTO;
	}
	
	
	@PutMapping("/decreaseQuantity")
	public CartDTO decreaseQuantity(@RequestParam Long inventoryId) {
		CartDTO cartDTO = cartService.decreaseQuantity(inventoryId);
		
		log.info("Decreasing the Quantity of item: {}", inventoryId);
		return cartDTO;
	}
	
	
	@PostMapping("/applyCoupon")
	public CartDTO applyCoupon(@RequestParam Long couponId) {
	    log.info("Applying coupon ID: {} to cart", couponId);
	    return cartService.applyCoupon(couponId);
	}

}
