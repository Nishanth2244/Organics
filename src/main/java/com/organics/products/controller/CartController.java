package com.organics.products.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
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
	public CartDTO addCart(@RequestBody AddToCartRequest addToCartRequest,
						@RequestParam Long customerId) {
		
		CartDTO cartDTO = cartService.addToCart(addToCartRequest, customerId);
		
		log.info("Item added to cart Succesfully: {}", addToCartRequest.getProductId());
		return cartDTO;
	}

}
