package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.model.enums.OrderStatus;
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

    Page<Order> findByOrderStatus(OrderStatus orderStatus, Pageable pageable);

    Page<Order> findByDriverIdAndOrderStatus(Long driverId, OrderStatus orderStatus, Pageable pageable);

    boolean existsByDriverIdAndOrderStatusIn(Long driverId, List<OrderStatus> orderStatus);

    @Query(value = "SELECT nextval('order_number_seq')", nativeQuery = true)
    Long getNextOrderSequence();

    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.createdAt >= :startOfDay AND o.createdAt < :endOfDay")
    Long countTodayOrders(@Param("startOfDay") LocalDateTime startOfDay,
                          @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT SUM(o.amount) FROM Order o " +
            "WHERE o.paymentStatus = 'PAID' AND o.createdAt " +
            "BETWEEN :startDate AND :endDate")
    BigDecimal calculateRevenue(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    Optional<Order> findByCustomerId(Long customerId);

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    int countByCustomerIdAndOrderStatus(Long customerId, OrderStatus orderStatus);

    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId ORDER BY o.createdAt DESC")
    Page<Order> findCustomerOrdersOrderByDateDesc(@Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.driver.id = :driverId")
    Long countByDriverId(@Param("driverId") Long driverId);

    @Query("SELECT o FROM Order o WHERE o.driver.id = :driverId ORDER BY o.createdAt DESC")
    Page<Order> findDriverOrdersOrderByDateDesc(@Param("driverId") Long driverId, Pageable pageable);
}
