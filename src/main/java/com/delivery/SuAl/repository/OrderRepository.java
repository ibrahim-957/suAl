package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.model.OrderStatus;
import com.delivery.SuAl.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByOrderStatus(OrderStatus orderStatus);

    Page<Order> findByOrderStatus(OrderStatus orderStatus, Pageable pageable);

    List<Order> findByOperatorId(Long operatorId);

    List<Order> findByDriverId(Long driverId);

    Page<Order> findByDriverIdAndOrderStatus(Long driverId, OrderStatus orderStatus, Pageable pageable);

    boolean existsByDriverIdAndOrderStatusIn(Long driverId, List<OrderStatus> orderStatus);

    Long countByOrderNumberStartingWith(String prefix);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(o.orderNumber, LENGTH(o.orderNumber) - 3, 4) AS integer)), 0) + 1 " +
            "FROM Order o " +
            "WHERE o.orderNumber LIKE CONCAT(:prefix, '%')")
    Long getNextOrderSequence(@Param("prefix") String prefix);

    @Query("SELECT o.orderNumber FROM Order o WHERE o.orderNumber LIKE CONCAT(:prefix, '%')")
    List<String> findOrderNumbersByPrefix(@Param("prefix") String prefix);

    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.createdAt >= :startOfDay AND o.createdAt < :endOfDay")
    Long countTodaysOrders(@Param("startOfDay") LocalDateTime startOfDay,
                           @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT SUM(o.amount) FROM Order o " +
            "WHERE o.paymentStatus = 'PAID' AND o.createdAt " +
            "BETWEEN :startDate AND :endDate")
    BigDecimal calculateRevenue(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);
}
