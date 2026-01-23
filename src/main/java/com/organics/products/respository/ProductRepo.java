
package com.organics.products.respository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.organics.products.entity.Product;

@Repository
public interface ProductRepo extends JpaRepository<Product, Long>{

	Page<Product> findByStatusTrue(Pageable pageable);

	Page<Product> findByStatusFalse(Pageable pageable);


	Page<Product> findByProductNameContainingIgnoreCaseAndStatusTrue(String name,Pageable pageable);

	List<Product> findByCategoryId(Long id);
}
