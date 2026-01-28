package com.organics.products.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.organics.products.entity.EntityType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

@Slf4j
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

		log.info("Creating coupon: {}", req.getCode());

		if (req.getCode() == null || req.getCode().isBlank()) {
			log.warn("Coupon creation failed: Code is empty");
			throw new BadRequestException("Coupon code cannot be empty");
		}

		if (req.getDiscountValue() == null || req.getDiscountValue() <= 0) {
			log.warn("Invalid discount value: {}", req.getDiscountValue());
			throw new BadRequestException("Discount value must be greater than zero");
		}

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

		Coupon saved = couponRepository.save(coupon);

		log.info("Coupon created successfully with id={}", saved.getId());
		try {
			notificationService.sendNotification(
					"ALL", // Receiver: "ALL" implies a broadcast (logic depends on your frontend handling)
					"New Coupon Available: " + saved.getCode() + " - " + saved.getDescription(),
					"ADMIN",
					"COUPON_ALERT",
					"/coupons",
					"Promotions",
					"General",
					"New Coupon Added!",
					EntityType.COUPON,
					saved.getId()
			);
		} catch (Exception e) {
			log.error("Failed to send coupon notification", e);
		}

		return converToDTO(saved);

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

		log.info("Fetching active coupons");

		List<Coupon> coupons = couponRepository.findByIsActiveTrue();

		if (coupons == null || coupons.isEmpty()) {
			log.warn("No active coupons found");
			return List.of();
		}

		log.info("Found {} active coupons", coupons.size());

		return coupons.stream()
				.map(this::converToDTO)
				.toList();
	}



	@Transactional(readOnly = true)
	public List<CouponDTO> getInActive() {

		log.info("Fetching inactive coupons");

		List<Coupon> coupons =
				couponRepository.findByIsActiveFalse();

		if (coupons == null || coupons.isEmpty()) {
			log.warn("No inactive coupons found");
			return List.of(); // safe empty list
		}

		log.info("Found {} inactive coupons", coupons.size());

		return coupons.stream()
				.map(this::converToDTO)
				.toList();
	}



	public void setStatus(Boolean status, Long id) {

		log.info("Updating coupon status: id={}, status={}", id, status);

		if (status == null) {
			log.warn("Status update failed: status is null");
			throw new BadRequestException("Status cannot be null");
		}

		Coupon coupon = couponRepository.findById(id)
				.orElseThrow(() -> {
					log.warn("Coupon not found for status update: {}", id);
					return new CouponNotFoundException("Coupon not found with id: " + id);
				});

		coupon.setActive(status);
		couponRepository.save(coupon);

		log.info("Coupon status updated successfully: id={}, status={}", id, status);
	}

	public void updateExtended(Long id,
							   String code,
							   String description,
							   DiscountType type,
							   Double val,
							   Double minAmt,
							   Double maxAmt,
							   Integer limit,
							   LocalDate start,
							   LocalDate end) {

		log.info("Updating coupon: id={}", id);

		Coupon coupon = couponRepository.findById(id)
				.orElseThrow(() -> {
					log.warn("Coupon not found to update: {}", id);
					return new CouponNotFoundException("Coupon not found with id: " + id);
				});

		if (code != null && !code.isBlank()) coupon.setCode(code);
		if (description != null) coupon.setDescription(description);
		if (type != null) coupon.setDiscountType(type);
		if (val != null) {
			if (val <= 0) {
				log.warn("Invalid discount value during update: {}", val);
				throw new BadRequestException("Discount value must be greater than zero");
			}
			coupon.setDiscountValue(val);
		}
		if (minAmt != null) coupon.setMinOrderAmount(minAmt);
		if (maxAmt != null) coupon.setMaxDiscountAmount(maxAmt);
		if (limit != null) coupon.setUsageLimit(limit);
		if (start != null) coupon.setStartDate(start);
		if (end != null) coupon.setEndDate(end);

		couponRepository.save(coupon);

		log.info("Coupon updated successfully: id={}", id);
	}
}
