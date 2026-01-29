package com.organics.products.respository;

import com.organics.products.entity.CategoryTax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryTaxRepository extends JpaRepository<CategoryTax, Long> {

    Optional<CategoryTax> findByCategory_Id(Long categoryId);
}
