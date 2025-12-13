package com.delivery.SuAl.controller;

import com.delivery.SuAl.model.request.operation.AssignDriverRequest;
import com.delivery.SuAl.model.request.operation.CompleteDeliveryRequest;
import com.delivery.SuAl.model.request.order.CreateOrderRequest;
import com.delivery.SuAl.model.request.order.UpdateOrderStatusRequest;
import com.delivery.SuAl.model.response.order.OrderResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1/api/orders")
@RequiredArgsConstructor
@Slf4j
@Validated
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest createOrderRequest) {

        OrderResponse response = orderService.createOrder(createOrderRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {

        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }



    @PutMapping("/{orderId}/assign-driver")
    public ResponseEntity<ApiResponse<OrderResponse>> assignDriver(
            @PathVariable Long orderId,
            @Valid @RequestBody AssignDriverRequest assignDriverRequest) {
        OrderResponse order = orderService.assignDriver(orderId, assignDriverRequest.getDriverId());
        return ResponseEntity.ok(ApiResponse.success("Order assigned successfully", order));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<OrderResponse>> approveOrder(@PathVariable Long id) {
        OrderResponse order = orderService.approveOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Order approved successfully", order));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<OrderResponse>> rejectOrder(
            @PathVariable Long id, @PathVariable String reason
    ) {
        OrderResponse order = orderService.approveOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Order approved successfully", order));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<OrderResponse>> completeOrder(
            @PathVariable Long id,
            @Valid @RequestBody CompleteDeliveryRequest completeDeliveryRequest
            ){
        OrderResponse order = orderService.completeOrder(id, completeDeliveryRequest);
        return ResponseEntity.ok(ApiResponse.success("Order completed successfully", order));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {

        orderService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Order deleted successfully", null));
    }

    @GetMapping("/today/count")
    public ResponseEntity<ApiResponse<Long>> getTodaysOrderCount() {

        Long count = orderService.countTodaysOrders();
        return ResponseEntity.ok(ApiResponse.success("Today's order count retrieved", count));
    }

    @GetMapping("/today/revenue")
    public ResponseEntity<ApiResponse<BigDecimal>> getTodaysRevenue() {

        BigDecimal revenue = orderService.calculateTodaysRevenue();
        return ResponseEntity.ok(ApiResponse.success("Today's revenue retrieved", revenue));
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<BigDecimal>> getRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        BigDecimal revenue = orderService.calculateRevenue(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Revenue calculated successfully", revenue));
    }
}
