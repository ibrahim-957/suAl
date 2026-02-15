package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("SELECT SUM(o.totalAmount) FROM Order o " +
            "WHERE o.paymentStatus = 'PAID' AND o.createdAt " +
            "BETWEEN :startDate AND :endDate")
    BigDecimal calculateRevenue(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    @Modifying
    @Query("UPDATE Order o SET o.address = null WHERE o.address.id = :addressId")
    void updateAddressToNullByAddressId(@Param("addressId") Long addressId);

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    int countByCustomerIdAndOrderStatus(Long customerId, OrderStatus orderStatus);

    @Query("SELECT DISTINCT o FROM Order o " +
            "JOIN o.orderDetails od " +
            "WHERE o.orderStatus = :status " +
            "AND od.product.company.id = :companyId")
    Page<Order> findByOrderStatusAndCompanyId(
            @Param("status") OrderStatus status,
            @Param("companyId") Long companyId,
            Pageable pageable
    );

    @Query("SELECT DISTINCT o FROM Order o " +
            "JOIN o.orderDetails od " +
            "WHERE od.product.company.id = :companyId")
    Page<Order> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.customer " +
            "LEFT JOIN FETCH o.driver " +
            "LEFT JOIN FETCH o.address " +
            "LEFT JOIN FETCH o.orderDetails od " +
            "LEFT JOIN FETCH od.product " +
            "WHERE o.id = :orderId")
    Optional<Order> findByIdWithDetails(@Param("orderId") Long orderId);

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.customer " +
            "LEFT JOIN FETCH o.orderDetails " +
            "WHERE o.customer.id = :customerId")
    Page<Order> findByCustomerIdWithDetails(@Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT o FROM Order o " +
            "JOIN FETCH o.customer c " +
            "JOIN FETCH c.user " +
            "WHERE o.orderStatus = :status")
    List<Order> findByOrderStatusWithCustomer(@Param("status") OrderStatus status);
}
