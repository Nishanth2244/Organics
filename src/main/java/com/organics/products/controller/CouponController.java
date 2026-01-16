package com.organics.products.controller;

import java.lang.module.ModuleDescriptor.Requires;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.organics.products.dto.CouponDTO;
import com.organics.products.dto.CreateCouponRequest;
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
	public CouponDTO createCoupon(@RequestBody CreateCouponRequest createCouponRequest) {
		
		CouponDTO couponDTO = couponService.createCoupon(createCouponRequest);
		log.info("Coupon Created Succesfully");
		return couponDTO;
	}
	
	
	@GetMapping("/get")
	public List<CouponDTO> getActive(){
		
		List<CouponDTO> coupons = couponService.getActive();
		return coupons;
	}
	
	
	@GetMapping("/getInActive")
	public List<CouponDTO> getInActive(){
		
		List<CouponDTO> coupons = couponService.getInActive();
		return coupons;
	}
	
	
	@PutMapping("/status/{id}")
	public String status(@PathVariable Long id,
						@RequestParam Boolean status) {
		
		couponService.setStatus(status, id);
		return "Status changed Succesfully";
		
	}
	
	
	
	@PutMapping("/update/{id}")
	public String update(@PathVariable Long id,
						@RequestParam(value = "code", required = false) String code, 
						@RequestParam(value = "discountPerce", required = false) Double discountPerc,
						@RequestParam(value = "maxDiscAmunt", required = false) Double maxDiscountAm,
						@RequestParam(value = "minOrderAmt", required = false) Double minOrderAmt) {
		
		couponService.update(id, code, discountPerc, maxDiscountAm, minOrderAmt);
		
		return "Coupon Updated succesfully";
	}
	
}
