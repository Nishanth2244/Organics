package com.organics.products.respository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.organics.products.entity.Category;

@Repository
public interface CategoryRepo extends JpaRepository<Category, Long> {

	boolean existsByCategoryName(String categoryName);

	Page<Category> findByStatusTrue(Pageable pageable);

	Page<Category> findByStatusFalse(Pageable pageable);

}
