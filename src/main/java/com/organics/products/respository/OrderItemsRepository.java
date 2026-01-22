// OrderItemsRepository.java
package com.organics.products.respository;

import com.organics.products.dto.TopOrderedProductsDTO;
import com.organics.products.entity.OrderItems;
import com.organics.products.entity.OrderStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderItemsRepository extends JpaRepository<OrderItems, Long> {
    List<OrderItems> findByOrderId(Long orderId);
    @Query("SELECT NEW com.organics.products.dto.TopOrderedProductsDTO(" +
            "p.id, p.productName, " +
            "SUM(oi.quantity), " +
            "COUNT(DISTINCT oi.order.id), " +
            "SUM(oi.quantity * oi.price)) " +
            "FROM OrderItems oi " +
            "JOIN oi.product p " +
            "JOIN oi.order o " +
            "WHERE o.orderStatus != com.organics.products.entity.OrderStatus.CANCELLED " +
            "GROUP BY p.id, p.productName " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<TopOrderedProductsDTO> findTopOrderedProducts();

    @Query("SELECT NEW com.organics.products.dto.TopOrderedProductsDTO(" +
            "p.id, p.productName, " +
            "SUM(oi.quantity), " +
            "COUNT(DISTINCT oi.order.id), " +
            "SUM(oi.quantity * oi.price)) " +
            "FROM OrderItems oi " +
            "JOIN oi.product p " +
            "JOIN oi.order o " +
            "WHERE o.orderStatus != com.organics.products.entity.OrderStatus.CANCELLED " +
            "AND o.orderDate BETWEEN :startDate AND :endDate " +
            "GROUP BY p.id, p.productName " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<TopOrderedProductsDTO> findTopOrderedProductsByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT NEW com.organics.products.dto.TopOrderedProductsDTO(" +
            "i.product.id, i.product.productName, " +
            "SUM(oi.quantity), " +
            "COUNT(DISTINCT oi.order.id), " +
            "SUM(oi.quantity * oi.price)) " +
            "FROM OrderItems oi " +
            "JOIN oi.product p " +
            "JOIN p.inventories i " +
            "JOIN oi.order o " +
            "WHERE o.orderStatus != com.organics.products.entity.OrderStatus.CANCELLED " +
            "AND i.id = :inventoryId " +
            "GROUP BY i.product.id, i.product.productName " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<TopOrderedProductsDTO> findTopOrderedProductsByInventoryId(
            @Param("inventoryId") Long inventoryId);
	boolean existsByOrderUserIdAndProductIdAndOrderOrderStatus(Long userId, Long productId, OrderStatus delivered);
}