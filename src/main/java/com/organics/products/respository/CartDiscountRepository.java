package com.organics.products.respository;

import com.organics.products.entity.CartDiscount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartDiscountRepository extends JpaRepository<CartDiscount, Long> {
    CartDiscount findByCartId(Long cartId);
}
