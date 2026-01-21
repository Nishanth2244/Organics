package com.organics.products.respository;

import com.organics.products.entity.Cart;
import com.organics.products.entity.CartItems;
import com.organics.products.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItems, Long> {


    Optional<CartItems> findByCartAndProduct(Cart cart, Product product);

    List<CartItems> findByCartId(Long cartId);


    @Query("SELECT ci FROM CartItems ci JOIN FETCH ci.product p WHERE ci.cart.id = :cartId")
    List<CartItems> findByCartIdWithProduct(@Param("cartId") Long cartId);


    void deleteByCartId(Long cartId);


    boolean existsByProductId(Long productId);


    Long countByCartId(Long cartId);
}