package com.delivery.SuAl.controller;

import com.delivery.SuAl.model.request.operation.CreateOperatorRequest;
import com.delivery.SuAl.model.request.operation.UpdateOperatorRequest;
import com.delivery.SuAl.model.response.operation.OperatorResponse;
import com.delivery.SuAl.model.response.order.OrderResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.service.OperatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/operators")
@RequiredArgsConstructor
@Slf4j
@Validated
public class OperatorController {
    private final OperatorService operatorService;

    @PostMapping
    public ResponseEntity<ApiResponse<OperatorResponse>> createOperator(
            @Valid @RequestBody CreateOperatorRequest createOperatorRequest) {
        OperatorResponse response = operatorService.createOperator(createOperatorRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Operator created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OperatorResponse>> getOperatorById(@PathVariable Long id) {
        OperatorResponse response = operatorService.getOperatorById(id);
        return ResponseEntity.ok(ApiResponse.success("Operator found successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OperatorResponse>> updateOperator(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOperatorRequest updateOperatorRequest) {
        OperatorResponse response = operatorService.updateOperator(id, updateOperatorRequest);
        return ResponseEntity.ok(ApiResponse.success("Operator updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<OperatorResponse>> deleteOperator(@PathVariable Long id) {
        operatorService.deleteOperator(id);
        return ResponseEntity.ok(ApiResponse.success("Operator deleted successfully", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OperatorResponse>>> getAllOperators(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction
    ){
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, sortDirection, sortBy);

        PageResponse<OperatorResponse> pageResponse = operatorService.getAllOperators(pageable);
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
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

        PageResponse<OrderResponse> pageResponse = operatorService.getPendingOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction
    ) {

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, sortDirection, sortBy);

        PageResponse<OrderResponse> orders = operatorService.getAllOrdersForManagement(pageable);

        return ResponseEntity.ok(ApiResponse.success(orders));
    }
}
