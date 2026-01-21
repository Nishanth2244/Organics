// OrderItemsRepository.java
package com.organics.products.respository;

import com.organics.products.entity.OrderItems;
import com.organics.products.entity.OrderStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemsRepository extends JpaRepository<OrderItems, Long> {
    List<OrderItems> findByOrderId(Long orderId);

	boolean existsByOrderUserIdAndProductIdAndOrderOrderStatus(Long userId, Long productId, OrderStatus delivered);
}