package com.organics.products.respository;

import com.organics.products.entity.WishListItems;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishListItemsRepository
        extends JpaRepository<WishListItems, Long> {

    boolean existsByWishlistIdAndProductId(Long wishlistId, Long productId);

    void deleteByWishlistIdAndProductId(Long wishlistId, Long productId);
}
