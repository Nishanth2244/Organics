package com.organics.products.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.organics.products.dto.CouponDTO;
import com.organics.products.entity.Coupon;
import com.organics.products.respository.CouponRepository;

@Service
public class CouponService {
	
	
	@Autowired
	private CouponRepository couponRepository;
	
	public CouponDTO converToDTO(Coupon coupon) {
		
		CouponDTO couponDTO = new CouponDTO();
		couponDTO.setId(coupon.getId());
		couponDTO.setActive(coupon.isActive());
		couponDTO.setCode(coupon.getCode());
		couponDTO.setDiscountPercentage(coupon.getDiscountPercentage());
		couponDTO.setExpiryDate(coupon.getExpiryDate());
		couponDTO.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
		couponDTO.setMinOrderAmount(coupon.getMinOrderAmount());
		
		return couponDTO;
	}

	public CouponDTO createCoupon(Coupon coupon) {
		
		Coupon savedCoupon = couponRepository.save(coupon);
		
		return converToDTO(savedCoupon);
	}

}
