package com.organics.products.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.organics.products.dto.CouponDTO;
import com.organics.products.dto.CreateCouponRequest;
import com.organics.products.entity.Coupon;
import com.organics.products.entity.DiscountType;
import com.organics.products.exception.CouponNotFoundException;
import com.organics.products.exception.BadRequestException;
import com.organics.products.respository.CouponRepository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CouponService {

	@Autowired
	private CouponRepository couponRepository;

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

		return converToDTO(saved);
	}

	@Transactional(readOnly = true)
	public Page<CouponDTO> getActive(int page, int size) {

		log.info("Fetching active coupons: page={}, size={}", page, size);

		Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

		Page<Coupon> couponsPage = couponRepository.findByIsActiveTrue(pageable);

		if (couponsPage.isEmpty()) {
			log.warn("No active coupons found");
			return Page.empty(pageable);
		}

		return couponsPage.map(this::converToDTO);
	}


	@Transactional(readOnly = true)
	public Page<CouponDTO> getInActive(int page, int size) {

		log.info("Fetching inactive coupons: page={}, size={}", page, size);

		Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

		Page<Coupon> couponsPage = couponRepository.findByIsActiveFalse(pageable);

		if (couponsPage.isEmpty()) {
			log.warn("No inactive coupons found");
			return Page.empty(pageable);
		}

		return couponsPage.map(this::converToDTO);
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
