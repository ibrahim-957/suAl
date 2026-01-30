package com.delivery.SuAl.controller;

import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.model.request.operation.AssignDriverRequest;
import com.delivery.SuAl.model.request.order.CompleteDeliveryRequest;
import com.delivery.SuAl.model.request.order.CreateOrderByCustomerRequest;
import com.delivery.SuAl.model.request.order.CreateOrderByOperatorRequest;
import com.delivery.SuAl.model.request.order.UpdateOrderRequest;
import com.delivery.SuAl.model.response.order.DriverCollectionInfoResponse;
import com.delivery.SuAl.model.response.order.OrderResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    @PostMapping("/create-by-customer")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrderByCustomer(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid CreateOrderByCustomerRequest request
            ){
        log.info("Order creation request from customer: {}", user.getPhoneNumber());

        OrderResponse response = orderService.createOrderByCustomer(user.getPhoneNumber(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", response));
    }

    @PostMapping("/create-by-operator")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrderByOperator(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid CreateOrderByOperatorRequest request
    ){
        log.info("Order creation by operator: {} for customerId: {}", user.getEmail(), request.getCustomerId());

        OrderResponse response = orderService.createOrderByOperator(user.getEmail(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        log.info("GET /v1/api/orders/{} - Fetching order", id);
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderRequest updateOrderRequest
    ) {
        log.info("PUT /v1/api/orders/{} - Updating order", id);
        OrderResponse response = orderService.updateOrder(id, updateOrderRequest);
        return ResponseEntity.ok(ApiResponse.success("Order updated successfully", response));
    }

    @PutMapping("/{orderId}/assign-driver")
    public ResponseEntity<ApiResponse<OrderResponse>> assignDriver(
            @PathVariable Long orderId,
            @Valid @RequestBody AssignDriverRequest assignDriverRequest
    ) {
        log.info("PUT /v1/api/orders/{}/assign-driver - Assigning driver: {}", orderId, assignDriverRequest.getDriverId());
        OrderResponse order = orderService.assignDriver(orderId, assignDriverRequest.getDriverId());
        return ResponseEntity.ok(ApiResponse.success("Driver assigned successfully", order));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<OrderResponse>> approveOrder(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        log.info("PATCH /v1/api/orders/{}/approve - Approving order", id);
        OrderResponse order = orderService.approveOrder(user.getEmail(), id);
        return ResponseEntity.ok(ApiResponse.success("Order approved successfully", order));
    }

    @PatchMapping("/customer/{id}/reject")
    public ResponseEntity<ApiResponse<OrderResponse>> rejectOrderByCustomer(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody String reason
    ){
        log.info("PATCH /v1/api/orders/customer/{}/reject", id);
        OrderResponse order = orderService.rejectOrderByCustomer(user.getPhoneNumber(), id, reason);
        return ResponseEntity.ok(ApiResponse.success("Order rejected successfully", order));
    }

    @PatchMapping("/operator/{id}/reject")
    public ResponseEntity<ApiResponse<OrderResponse>> rejectOrderByOperator(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody String reason
    ) {
        log.info("PATCH /v1/api/orders/operaot/{}/reject - Rejecting order with reason: {}", id, reason);
        OrderResponse order = orderService.rejectOrderByOperator(user.getEmail(), id, reason);
        return ResponseEntity.ok(ApiResponse.success("Order rejected successfully", order));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<OrderResponse>> completeOrder(
            @PathVariable Long id,
            @Valid @RequestBody CompleteDeliveryRequest completeDeliveryRequest
    ) {
        log.info("PUT /v1/api/orders/{}/complete - Completing order", id);
        OrderResponse order = orderService.completeOrder(id, completeDeliveryRequest);
        return ResponseEntity.ok(ApiResponse.success("Order completed successfully", order));
    }

    @GetMapping("/today/count")
    public ResponseEntity<ApiResponse<Long>> getTodayOrderCount() {
        log.info("GET /v1/api/orders/today/count - Fetching today's order count");
        Long count = orderService.countTodayOrders();
        return ResponseEntity.ok(ApiResponse.success("Today's order count retrieved", count));
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<BigDecimal>> getRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        log.info("GET /v1/api/orders/revenue - Calculating revenue from {} to {}", startDate, endDate);
        BigDecimal revenue = orderService.calculateRevenue(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Revenue calculated successfully", revenue));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getPendingOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction
    ) {

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, sortDirection, sortBy);

        PageResponse<OrderResponse> pageResponse = orderService.getPendingOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction
    ) {

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, sortDirection, sortBy);

        PageResponse<OrderResponse> orders = orderService.getAllOrdersForManagement(pageable);

        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllCustomerOrders(
            Pageable pageable,
            @AuthenticationPrincipal User user
    ){
        log.info("GET /v1/api/orders/my-orders - Fetching order for customer by phoneNumber: {}", user.getPhoneNumber());
        PageResponse<OrderResponse> response = orderService.getAllOrdersByCustomer(pageable, user.getPhoneNumber());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{orderId}/driver-collection-info")
    public ResponseEntity<ApiResponse<DriverCollectionInfoResponse>> getDriverCollectionInfo(
            @PathVariable Long orderId
    ){
        log.info("GET /api/orders/{}/driver-collection-info - Fetching driver collection info", orderId);

        DriverCollectionInfoResponse response = orderService.getDriverCollectionInfo(orderId);

        if (Boolean.TRUE.equals(response.getHasInsufficientContainers())){
            log.warn("Order {} has insufficient containers warning for driver", orderId);
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}