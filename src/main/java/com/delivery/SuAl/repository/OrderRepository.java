package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    @Query("""
        SELECT o
        FROM Order o
        LEFT JOIN FETCH o.orderDetails
        LEFT JOIN FETCH o.address
        LEFT JOIN FETCH o.operator
        LEFT JOIN FETCH o.driver
        LEFT JOIN FETCH o.promo
        WHERE o.id = :id
    """)
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT o.orderNumber FROM Order o " +
            "WHERE o.orderNumber LIKE CONCAT(:prefix, '%')")
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
