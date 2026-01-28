package com.organics.products.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.organics.products.config.SecurityUtil;
import com.organics.products.dto.CouponDTO;
import com.organics.products.dto.CreateCouponRequest;
import com.organics.products.entity.Coupon;
import com.organics.products.entity.DiscountType;
import com.organics.products.entity.User;
import com.organics.products.exception.ResourceNotFoundException;
import com.organics.products.respository.CartRepository;
import com.organics.products.respository.CouponRepository;
import com.organics.products.respository.UserRepository;

@Service
public class CouponService {

	@Autowired
	private CouponRepository couponRepository;
	
	@Autowired
	private UserRepository customerRepository;
	
	@Autowired
	private CartRepository cartRepository;

	public CouponDTO converToDTO(Coupon coupon) {
		CouponDTO dto = new CouponDTO();
		dto.setId(coupon.getId());
		dto.setCode(coupon.getCode());
		dto.setDescription(coupon.getDescription());
		dto.setDiscountType(coupon.getDiscountType());
		dto.setDiscountValue(coupon.getDiscountValue());
		dto.setMinOrderAmount(coupon.getMinOrderAmount());
		dto.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
		dto.setUsageLimit(coupon.getUsageLimit());
		dto.setUsedCount(coupon.getUsedCount());
		dto.setStartDate(coupon.getStartDate());
		dto.setEndDate(coupon.getEndDate());
		dto.setActive(coupon.isActive());
		return dto;
	}

	public CouponDTO createCoupon(CreateCouponRequest req) {
		Coupon coupon = new Coupon();
		coupon.setCode(req.getCode());
		coupon.setDescription(req.getDescription());
		coupon.setDiscountType(req.getDiscountType());
		coupon.setDiscountValue(req.getDiscountValue());
		coupon.setMinOrderAmount(req.getMinOrderAmount());
		coupon.setMaxDiscountAmount(req.getMaxDiscountAmount());
		coupon.setUsageLimit(req.getUsageLimit());
		coupon.setStartDate(req.getStartDate());
		coupon.setEndDate(req.getEndDate());
		coupon.setActive(req.isActive());
		coupon.setUsedCount(0); 

		Coupon savedCoupon = couponRepository.save(coupon);
		return converToDTO(savedCoupon);
	}
	

	public List<CouponDTO> getActive() {
		
	    Long userId = SecurityUtil.getCurrentUserId()
	            .orElseThrow(() -> new RuntimeException("Unauthorized"));

	    User user = customerRepository.findById(userId)
	            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

	    List<Long> usedCouponIds = cartRepository.findByUser(user).stream()
	            .flatMap(cart -> cart.getAppliedCoupons().stream())
	            .map(cartCoupon -> cartCoupon.getCoupon().getId())
	            .collect(Collectors.toList());

	    return couponRepository.findByIsActiveTrue().stream()
	            .filter(coupon -> !usedCouponIds.contains(coupon.getId())) 
	            .map(this::converToDTO)
	            .collect(Collectors.toList());
	}

	public List<CouponDTO> getInActive() {
		return couponRepository.findByIsActiveFalse().stream().map(this::converToDTO).collect(Collectors.toList());
	}

	public void setStatus(Boolean status, Long id) {
		Coupon coupon = couponRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Coupon Not Found"));
		coupon.setActive(status);
		couponRepository.save(coupon);
	}

	public void updateExtended(Long id, String code, String description, DiscountType type, Double val, Double minAmt,
			Double maxAmt, Integer limit, LocalDate start, LocalDate end) {

		Coupon coupon = couponRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Coupon Not found to Update: " + id));

		if (code != null)
			coupon.setCode(code);
		if (description != null)
			coupon.setDescription(description);
		if (type != null)
			coupon.setDiscountType(type);
		if (val != null)
			coupon.setDiscountValue(val);
		if (minAmt != null)
			coupon.setMinOrderAmount(minAmt);
		if (maxAmt != null)
			coupon.setMaxDiscountAmount(maxAmt);
		if (limit != null)
			coupon.setUsageLimit(limit);
		if (start != null)
			coupon.setStartDate(start);
		if (end != null)
			coupon.setEndDate(end);

		couponRepository.save(coupon);
	}
}