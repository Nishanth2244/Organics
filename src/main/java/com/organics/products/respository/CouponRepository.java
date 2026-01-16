package com.organics.products.respository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.organics.products.entity.Coupon;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

}
