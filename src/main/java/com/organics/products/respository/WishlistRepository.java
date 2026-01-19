package com.organics.products.respository;

import com.organics.products.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByUserId(Long userId);

    @Query("""
        SELECT DISTINCT w FROM Wishlist w
        LEFT JOIN FETCH w.wishListItems wi
        LEFT JOIN FETCH wi.product p
        LEFT JOIN FETCH p.images
        WHERE w.user.id = :userId
    """)
    Optional<Wishlist> findByUserIdWithProducts(Long userId);
}
