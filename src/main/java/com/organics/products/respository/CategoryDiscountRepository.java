package com.organics.products.respository;

import com.organics.products.entity.CategoryDiscount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryDiscountRepository extends JpaRepository<CategoryDiscount, Long> {
    CategoryDiscount findByCategoryId(Long categoryId);

    boolean existsByCategoryId(Long categoryId);
}
