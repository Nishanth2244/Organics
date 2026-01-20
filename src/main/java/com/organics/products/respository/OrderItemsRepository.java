package com.organics.products.respository;

import com.organics.products.entity.Order;
import com.organics.products.entity.OrderItems;
import com.organics.products.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface OrderItemsRepository extends JpaRepository<OrderItems, Long> {

    boolean existsByOrderUserIdAndProductIdAndOrderOrderStatus(
            Long userId, Long productId, OrderStatus orderStatus);

}


