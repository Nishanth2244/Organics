package com.organics.products.respository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.organics.products.entity.CartItems;

@Repository
public interface CartItemRepository extends JpaRepository<CartItems, Long> {


//    Optional<CartItems> findByCartAndProduct(Cart cart, Product product);

    List<CartItems> findByCartId(Long cartId);


    @Query("SELECT ci FROM CartItems ci JOIN FETCH ci.inventory WHERE ci.cart.id = :cartId")
    List<CartItems> findByCartIdWithProduct(@Param("cartId") Long cartId);


    void deleteByCartId(Long cartId);


//    boolean existsByProductId(Long productId);


    Long countByCartId(Long cartId);
}