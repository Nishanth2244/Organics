package com.organics.products.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.organics.products.dto.CouponDTO;
import com.organics.products.entity.Coupon;
import com.organics.products.service.CouponService;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/api/organics/coupon")
public class CouponController {
	
	
	@Autowired
	private CouponService couponService;
	
	@PostMapping("/create")
	public CouponDTO createCoupon(@RequestBody Coupon coupon) {
		
		CouponDTO couponDTO = couponService.createCoupon(coupon);
		log.info("Coupon Created Succesfully");
		return couponDTO;
	}
	
}
