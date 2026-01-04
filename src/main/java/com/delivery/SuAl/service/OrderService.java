package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.basket.CreateOrderFromBasketByOperatorRequest;
import com.delivery.SuAl.model.request.basket.CreateOrderFromBasketRequest;
import com.delivery.SuAl.model.request.order.CompleteDeliveryRequest;
import com.delivery.SuAl.model.request.order.UpdateOrderRequest;
import com.delivery.SuAl.model.response.order.OrderResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface OrderService {
    OrderResponse createOrderFromBasketByUser(String phoneNumber, CreateOrderFromBasketRequest request);

    OrderResponse createOrderFromBasketByOperator(String operatorEmail, CreateOrderFromBasketByOperatorRequest request);

    OrderResponse updateOrder(Long orderId, UpdateOrderRequest request);

    OrderResponse getOrderById(Long id);

    OrderResponse assignDriver(Long orderId, Long driverId);

    OrderResponse approveOrder(String operatorEmail, Long orderId);

    OrderResponse rejectOrder(String operatorEmail, Long orderId, String reason);

    OrderResponse completeOrder(Long orderId, CompleteDeliveryRequest completeDeliveryRequest);

    Long countTodayOrders();

    BigDecimal calculateRevenue(LocalDateTime startDate, LocalDateTime endDate);

    PageResponse<OrderResponse> getPendingOrders(Pageable pageable);

    PageResponse<OrderResponse> getAllOrdersForManagement(Pageable pageable);

}
