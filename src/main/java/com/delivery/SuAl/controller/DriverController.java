package com.delivery.SuAl.controller;

import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.model.request.operation.CreateDriverRequest;
import com.delivery.SuAl.model.request.operation.UpdateDriverRequest;
import com.delivery.SuAl.model.response.auth.AuthenticationResponse;
import com.delivery.SuAl.model.response.operation.DriverResponse;
import com.delivery.SuAl.model.response.order.OrderResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.service.DriverService;
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
@RequestMapping("/v1/api/drivers")
@RequiredArgsConstructor
@Slf4j
@Validated
public class DriverController {
    private final DriverService driverService;

    @PostMapping
    public ResponseEntity<ApiResponse<AuthenticationResponse>> createDriver(
            @Valid @RequestBody CreateDriverRequest createDriverRequest) {
        AuthenticationResponse driverResponse = driverService.createDriver(createDriverRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Driver created successfully", driverResponse));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverResponse>> updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDriverRequest updateDriverRequest) {
        DriverResponse driverResponse = driverService.updateDriver(id, updateDriverRequest);

        return ResponseEntity.ok(ApiResponse.success("Driver updated successfully", driverResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverResponse>> getDriverById(@PathVariable Long id) {
        DriverResponse driverResponse = driverService.getDriverById(id);

        return ResponseEntity.ok(ApiResponse.success("Driver retrieved successfully", driverResponse));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverResponse>> deleteDriver(@PathVariable Long id) {
        driverService.deleteDriver(id);

        return ResponseEntity.ok(ApiResponse.success("Driver deleted successfully", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DriverResponse>>> getAllDrivers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, sortDirection, sortBy);

        PageResponse<DriverResponse> driverResponsePageResponse = driverService.getAllDrivers(pageable);
        return ResponseEntity.ok(ApiResponse.success(driverResponsePageResponse));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyAssignedOrders(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<OrderResponse> orders = driverService.getMyAssignedOrders(user.getEmail(), pageable);

        return ResponseEntity.ok(ApiResponse.success(orders));
    }
}
