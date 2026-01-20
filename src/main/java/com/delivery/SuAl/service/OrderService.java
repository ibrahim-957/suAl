package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.model.request.order.CompleteDeliveryRequest;
import com.delivery.SuAl.model.request.order.CreateOrderByOperatorRequest;
import com.delivery.SuAl.model.request.order.CreateOrderByUserRequest;
import com.delivery.SuAl.model.request.order.UpdateOrderRequest;
import com.delivery.SuAl.model.response.order.OrderResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import org.aspectj.weaver.ast.Or;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface OrderService {
    OrderResponse createOrderByUser(String phoneNumber, CreateOrderByUserRequest request);

    OrderResponse createOrderByOperator(String operatorEmail, CreateOrderByOperatorRequest request);

    OrderResponse updateOrder(Long orderId, UpdateOrderRequest request);

    OrderResponse getOrderById(Long id);

    OrderResponse assignDriver(Long orderId, Long driverId);

    OrderResponse approveOrder(String operatorEmail, Long orderId);

    OrderResponse rejectOrderByUser(String phoneNumber, Long orderId, String reason);

    OrderResponse rejectOrderByOperator(String operatorEmail, Long orderId, String reason);

    OrderResponse completeOrder(Long orderId, CompleteDeliveryRequest completeDeliveryRequest);

    Long countTodayOrders();

    BigDecimal calculateRevenue(LocalDateTime startDate, LocalDateTime endDate);

    PageResponse<OrderResponse> getPendingOrders(Pageable pageable);

    PageResponse<OrderResponse> getAllOrdersForManagement(Pageable pageable);

    PageResponse<OrderResponse> getAllOrdersByUser(Pageable pageable, String phoneNumber);

    int getCompletedOrderCount(Long userId);

    Order getOrderEntityById(Long orderId);
}
