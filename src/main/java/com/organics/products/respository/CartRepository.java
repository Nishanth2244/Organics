package com.organics.products.respository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.organics.products.entity.Cart;
import com.organics.products.entity.User;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long>{

	List<Cart> findByUserAndIsActive(User customer, boolean b);

}
