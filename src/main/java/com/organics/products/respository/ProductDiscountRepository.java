package com.organics.products.respository;

import com.organics.products.entity.ProductDiscount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductDiscountRepository extends JpaRepository<ProductDiscount, Long> {
    List<ProductDiscount> findByProductId(Long productId);

    void deleteByProductId(Long productId);

    boolean existsByProductId(Long productId);
}
