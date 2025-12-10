package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.order.AssignDriverRequest;
import com.delivery.SuAl.model.request.order.CreateOrderRequest;
import com.delivery.SuAl.model.request.order.UpdateOrderStatusRequest;
import com.delivery.SuAl.model.response.order.OrderResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest createOrderRequest);

    OrderResponse getOrderById(Long id);

    OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest updateOrderStatusRequest);

    void deleteOrder(Long id);

    OrderResponse assignDriver(Long id, AssignDriverRequest assignDriverRequest);

    Long countTodaysOrders();

    BigDecimal calculateRevenue(LocalDateTime startDate, LocalDateTime endDate);

    BigDecimal calculateTodaysRevenue();
}
