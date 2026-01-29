package com.organics.products.respository;

import com.organics.products.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepo extends JpaRepository<Category, Long> {

    boolean existsByCategoryName(String categoryName);

    Page<Category> findByStatusTrue(Pageable pageable);

    Page<Category> findByStatusFalse(Pageable pageable);

    Optional<Category> findByCategoryNameIgnoreCase(String categoryName);
}
