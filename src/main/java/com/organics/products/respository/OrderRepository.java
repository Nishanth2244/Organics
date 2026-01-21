package com.organics.products.respository;

import com.organics.products.entity.Order;
import com.organics.products.entity.OrderStatus;
import com.organics.products.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserOrderByOrderDateDesc(User user);

    List<Order> findByUserId(Long userId);

    List<Order> findByOrderStatusOrderByOrderDateDesc(OrderStatus status);

    List<Order> findAllByOrderByOrderDateDesc();

    List<Order> findByOrderDateOrderByOrderDateDesc(LocalDate date);

    @Query("SELECT o.orderDate, COUNT(o), SUM(o.orderAmount), " +
            "SUM(CASE WHEN o.orderStatus = 'PENDING' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN o.orderStatus = 'DELIVERED' THEN 1 ELSE 0 END) " +
            "FROM Order o " +
            "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
            "GROUP BY o.orderDate " +
            "ORDER BY o.orderDate")
    List<Object[]> getDailyOrderStats(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(o.orderAmount), 0) FROM Order o WHERE o.orderDate = :date")
    Double sumOrderAmountByDate(@Param("date") LocalDate date);

    // Count queries for date range
    Long countByOrderDateBetween(LocalDate startDate, LocalDate endDate);
    Long countByOrderDate(LocalDate date);

    @Query("SELECT o.orderStatus, COUNT(o) FROM Order o WHERE o.orderDate = :date GROUP BY o.orderStatus")
    List<Object[]> countByStatusAndDate(@Param("date") LocalDate date);

    @Query("SELECT o.orderStatus, COUNT(o) FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate GROUP BY o.orderStatus")
    List<Object[]> countByStatusAndDateRange(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    // Daily order counts with amounts
    @Query("SELECT o.orderDate, COUNT(o), COALESCE(SUM(o.orderAmount), 0) " +
            "FROM Order o " +
            "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
            "GROUP BY o.orderDate " +
            "ORDER BY o.orderDate")
    List<Object[]> getDailyOrderCounts(@Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);
    @Query("SELECT COALESCE(SUM(o.orderAmount), 0) FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    Double sumOrderAmountByDateRange(@Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);
    List<Order> findByOrderDateBetweenOrderByOrderDateDesc(LocalDate startDate, LocalDate endDate);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems i LEFT JOIN FETCH i.product WHERE o.id = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);

    @Query("SELECT o FROM Order o WHERE o.orderDate >= :startDate AND o.orderDate <= :endDate ORDER BY o.orderDate DESC")
    List<Order> findByOrderDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus = :status")
    Long countByStatus(@Param("status") OrderStatus status);

    @Query("SELECT SUM(o.orderAmount) FROM Order o WHERE o.orderStatus = :status AND o.orderDate >= :startDate")
    Double getRevenueByStatusAndDate(@Param("status") OrderStatus status, @Param("startDate") LocalDate startDate);
}