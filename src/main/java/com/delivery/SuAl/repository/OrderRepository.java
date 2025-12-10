package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.model.OrderStatus;
import com.delivery.SuAl.model.PaymentStatus;
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

    List<Order> findByOperatorId(Long operatorId);

    List<Order> findByDriverId(Long driverId);

    boolean existsByDriverIdAndOrderStatusIn(Long driverId, List<OrderStatus> orderStatus);
}
