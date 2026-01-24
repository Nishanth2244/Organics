package com.organics.products.respository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.organics.products.dto.CouponDTO;
import com.organics.products.entity.Coupon;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

	List<Coupon> findByIsActiveTrue( );

	Page<Coupon> findByIsActiveFalse(Pageable  pageable);

	Coupon findByCode(String couponCode);
}
