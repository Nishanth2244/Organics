package com.organics.products.respository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.organics.products.entity.Cart;
import com.organics.products.entity.User;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long>{


	List<Cart> findByUserAndIsActive(User user, boolean isActive);


	@Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.product WHERE c.id = :cartId")
	Optional<Cart> findByIdWithItems(@Param("cartId") Long cartId);

	@Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.product WHERE c.user = :user AND c.isActive = true")
	List<Cart> findByUserAndIsActiveWithItems(@Param("user") User user, boolean isActive);

	@Query("SELECT DISTINCT c FROM Cart c " +
			"LEFT JOIN FETCH c.items i " +
			"LEFT JOIN FETCH i.product p " +
			"WHERE c.user.id = :userId AND c.isActive = true")
	Optional<Cart> findByIdWithItemsAndUser(@Param("userId") Long userId);

}
