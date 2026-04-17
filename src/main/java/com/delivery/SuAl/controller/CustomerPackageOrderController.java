package com.delivery.SuAl.controller;

import com.delivery.SuAl.entity.Customer;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.model.dto.payment.PaymentDTO;
import com.delivery.SuAl.model.request.affordablepackage.OrderAffordablePackageRequest;
import com.delivery.SuAl.model.response.affordablepackage.CustomerPackageOrderResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.CustomerRepository;
import com.delivery.SuAl.service.CustomerPackageOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/package-orders")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CustomerPackageOrderController {
    private final CustomerPackageOrderService customerPackageOrderService;
    private final CustomerRepository customerRepository;


    @PostMapping
    public ResponseEntity<ApiResponse<CustomerPackageOrderResponse>> orderPackage(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid OrderAffordablePackageRequest request) {
        Long customerId = resolveCustomerId(user);
        log.info("POST /v1/api/package-orders - Customer {} ordering package: {}",
                customerId, request.getPackageId());

        CustomerPackageOrderResponse response =
                customerPackageOrderService.orderPackage(customerId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Package order created successfully", response));
    }

    @PostMapping("/{packageOrderId}/initialize-payment")
    public ResponseEntity<ApiResponse<PaymentDTO>> initializePackagePayment(
            @PathVariable Long packageOrderId,
            @RequestParam(required = false, defaultValue = "az") String language
    ) {
        log.info("POST /v1/api/package-orders/{}/initialize-payment - Initializing payment", packageOrderId);

        PaymentDTO paymentDTO = customerPackageOrderService.initializePackagePayment(packageOrderId, language);
        return ResponseEntity.ok(ApiResponse.success("Payment initialized successfully", paymentDTO));
    }

    @GetMapping("/my-package-orders")
    public ResponseEntity<ApiResponse<PageResponse<CustomerPackageOrderResponse>>> getCustomerPackageOrders(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        Long customerId = resolveCustomerId(user);
        log.info("GET /v1/api/package-orders/my-package-orders - Fetching orders for customer: {}",
                customerId);

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, sortDirection, sortBy);

        PageResponse<CustomerPackageOrderResponse> response =
                customerPackageOrderService.getCustomerPackageOrders(customerId, pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{packageOrderId}")
    public ResponseEntity<ApiResponse<CustomerPackageOrderResponse>> getPackageOrderById(
            @PathVariable Long packageOrderId
    ) {
        log.info("GET /v1/api/package-orders/{} - Fetching package order", packageOrderId);

        CustomerPackageOrderResponse response = customerPackageOrderService.getPackageOrderById(packageOrderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{packageOrderId}/cancel")
    public ResponseEntity<ApiResponse<CustomerPackageOrderResponse>> cancelPackageOrder(
            @AuthenticationPrincipal User user,
            @PathVariable Long packageOrderId
    ) {
        Long customerId = resolveCustomerId(user);
        log.info("PATCH /v1/api/package-orders/{}/cancel - Customer {} cancelling package order",
                packageOrderId, user.getId());

        CustomerPackageOrderResponse response = customerPackageOrderService.cancelPackageOrder(
                customerId, packageOrderId);

        return ResponseEntity.ok(ApiResponse.success("Package order cancelled successfully", response));
    }

    @PatchMapping("/{packageOrderId}/auto-renew")
    public ResponseEntity<ApiResponse<CustomerPackageOrderResponse>> toggleAutoRenew(
            @AuthenticationPrincipal User user,
            @PathVariable Long packageOrderId,
            @RequestParam Boolean autoRenew
    ) {
        Long customerId = resolveCustomerId(user);
        log.info("PATCH /v1/api/package-orders/{}/auto-renew - Setting auto-renew to: {}",
                packageOrderId, autoRenew);

        CustomerPackageOrderResponse response = customerPackageOrderService.toggleAutoRenew(
                customerId, packageOrderId, autoRenew);

        return ResponseEntity.ok(ApiResponse.success("Auto-renew setting updated successfully", response));
    }

    private Long resolveCustomerId(User user) {
        return customerRepository.findByUserId(user.getId())
                .map(Customer::getId)
                .orElseThrow(() -> new NotFoundException(
                        "Customer not found for user: " + user.getId()));
    }
}