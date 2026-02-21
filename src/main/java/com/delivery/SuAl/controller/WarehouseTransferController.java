package com.delivery.SuAl.controller;

import com.delivery.SuAl.model.request.transfer.CreateWarehouseTransferRequest;
import com.delivery.SuAl.model.request.transfer.UpdateWarehouseTransferRequest;
import com.delivery.SuAl.model.response.transfer.WarehouseTransferResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.service.WarehouseTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/warehouse-transfers")
@RequiredArgsConstructor
@Slf4j
@Validated
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
public class WarehouseTransferController {
    private final WarehouseTransferService warehouseTransferService;

    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseTransferResponse>> createWarehouseTransfer(
            @Valid @RequestBody CreateWarehouseTransferRequest request){
        log.info("POST /v1/api/warehouse-transfers - {} -> {}",
                request.getFromWarehouseId(), request.getToWarehouseId());

        WarehouseTransferResponse response = warehouseTransferService.createTransfer(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseTransferResponse>> getTransferById(
            @PathVariable Long id) {
        log.info("GET /v1/api/warehouse-transfers/{}", id);

        WarehouseTransferResponse response = warehouseTransferService.getTransferById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseTransferResponse>> updateTransfer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWarehouseTransferRequest request) {
        log.info("PUT /v1/api/warehouse-transfers/{}", id);

        WarehouseTransferResponse response = warehouseTransferService.updateTransfer(id, request);
        return ResponseEntity.ok(ApiResponse.success("Warehouse transfer updated successfully", response));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WarehouseTransferResponse>> completeTransfer(
            @PathVariable Long id) {
        log.info("POST /v1/api/warehouse-transfers/{}/complete", id);

        WarehouseTransferResponse response = warehouseTransferService.completeTransfer(id);
        return ResponseEntity.ok(ApiResponse.success("Warehouse transfer completed successfully", response));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<WarehouseTransferResponse>> cancelTransfer(
            @PathVariable Long id) {
        log.info("POST /v1/api/warehouse-transfers/{}/cancel", id);

        WarehouseTransferResponse response = warehouseTransferService.cancelTransfer(id);
        return ResponseEntity.ok(ApiResponse.success("Warehouse transfer cancelled successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<WarehouseTransferResponse>>> getAllTransfers(
            @ParameterObject Pageable pageable) {
        log.info("GET /v1/api/warehouse-transfers");

        PageResponse<WarehouseTransferResponse> response = warehouseTransferService.getAllTransfers(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
