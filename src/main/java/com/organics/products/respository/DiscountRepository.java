package com.organics.products.respository;

import com.organics.products.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DiscountRepository extends JpaRepository<Discount, Long> {
    List<Discount> findByActiveTrueAndValidToBefore(LocalDateTime now);
}
