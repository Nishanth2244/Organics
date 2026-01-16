package com.organics.products.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.organics.products.dto.CouponDTO;
import com.organics.products.dto.CreateCouponRequest;
import com.organics.products.entity.Coupon;
import com.organics.products.exception.ResourceNotFoundException;
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
	
	

	public CouponDTO createCoupon(CreateCouponRequest createCouponRequest) {
		
		Coupon coupon = new Coupon();
		coupon.setActive(createCouponRequest.isActive());
		coupon.setCode(createCouponRequest.getCode());
		coupon.setDiscountPercentage(createCouponRequest.getDiscountPercentage());
		coupon.setExpiryDate(createCouponRequest.getExpiryDate());
		coupon.setMaxDiscountAmount(createCouponRequest.getMaxDiscountAmount());
		coupon.setMinOrderAmount(createCouponRequest.getMinOrderAmount());
		
		Coupon savedCoupon = couponRepository.save(coupon);
		return converToDTO(savedCoupon);
	}



	public List<CouponDTO> getActive() {

	    return couponRepository.findByIsActiveTrue()
	            .stream()
	            .map(this::converToDTO)
	            .collect(Collectors.toList());
	}



	public List<CouponDTO> getInActive() {
		
		return couponRepository.findByIsActiveFalse()
	            .stream()
	            .map(this::converToDTO)
	            .collect(Collectors.toList());
				
		
	}



	public void setStatus(Boolean status, Long id) {
		
		Coupon coupon = couponRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Coupon Not Found to change status"));
		
		coupon.setActive(status);
		couponRepository.save(coupon);
		
	}



	public void update(Long id, String code, Double discountPerc, Double maxDiscountAm,
			Double minOrderAmt) {
		
		Coupon coupon = couponRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Coupon Not found to Update: "+ id));
		
		if(code != null) {
			coupon.setCode(code);
		}
		if(discountPerc != null) {
			coupon.setDiscountPercentage(discountPerc);
		}
		
		if(maxDiscountAm != null) {
			coupon.setMaxDiscountAmount(maxDiscountAm);
		}
		
		if(minOrderAmt != null) {
			coupon.setMinOrderAmount(minOrderAmt);
		}
		
		couponRepository.save(coupon);
		 
	}

}
