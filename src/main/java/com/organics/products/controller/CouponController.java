package com.organics.products.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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
import com.organics.products.entity.DiscountType;
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
        log.info("Creating coupon with code: {}", createCouponRequest.getCode());
        return couponService.createCoupon(createCouponRequest);
    }

    @GetMapping("/get")
    public List<CouponDTO> getActive() {
        return couponService.getActive();
    }

    @GetMapping("/getInActive")
    public List<CouponDTO> getInActive() {
        return couponService.getInActive();
    }

    @PutMapping("/status/{id}")
    public String updateStatus(@PathVariable Long id, @RequestParam Boolean status) {
        couponService.setStatus(status, id);
        log.info("Status changed for coupon ID: {} to {}", id, status);
        return "Status changed Successfully";
    }

    @PutMapping("/update/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) DiscountType discountType,
            @RequestParam(required = false) Double discountValue,
            @RequestParam(required = false) Double minOrderAmt,
            @RequestParam(required = false) Double maxDiscountAm,
            @RequestParam(required = false) Integer usageLimit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        couponService.updateExtended(id, code, description, discountType, discountValue, 
                                     minOrderAmt, maxDiscountAm, usageLimit, startDate, endDate);

        log.info("Coupon ID: {} updated with new attributes", id);
        return "Coupon Updated successfully";
    }
}