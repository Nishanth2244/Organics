package com.organics.products.respository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.organics.products.entity.CartItems;

@Repository
public interface CartItemRepository extends JpaRepository<CartItems, Long> {

}
