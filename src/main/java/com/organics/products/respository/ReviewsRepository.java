package com.organics.products.respository;

import com.organics.products.entity.Reviews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewsRepository extends JpaRepository<Reviews, Long> {

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    List<Reviews> findByProductIdAndIsEligibleTrue(Long productId);

    @Query("""
    SELECT COUNT(r), COALESCE(AVG(r.rating), 0)
    FROM Reviews r
    WHERE r.product.id = :productId
      AND r.isEligible = true
""")
    List<Object[]> getRatingSummary(@Param("productId") Long productId);

}
