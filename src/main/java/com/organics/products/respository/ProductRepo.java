package com.organics.products.respository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.organics.products.entity.Product;

@Repository
public interface ProductRepo extends JpaRepository<Product, Long>{

	List<Product> findByStatusTrue();

	List<Product> findByStatusFalse();

	List<Product> findByCategoryId(Long id);


}
