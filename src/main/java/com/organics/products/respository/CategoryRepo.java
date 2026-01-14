package com.organics.products.respository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.organics.products.entity.Category;

@Repository
public interface CategoryRepo extends JpaRepository<Category, Long> {

	boolean existsByCategoryName(String categoryName);

	List<Category> findByStatusTrue();

	List<Category> findByStatusFalse();

}
