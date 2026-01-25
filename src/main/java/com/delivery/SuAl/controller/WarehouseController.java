package com.delivery.SuAl.controller;

import com.delivery.SuAl.model.request.warehouse.CreateWarehouseRequest;
import com.delivery.SuAl.model.request.warehouse.UpdateStockRequest;
import com.delivery.SuAl.model.response.warehouse.WarehouseResponse;
import com.delivery.SuAl.model.response.warehouse.WarehouseStockResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.service.WarehouseService;
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

import java.util.List;

@RestController
@RequestMapping("/v1/api/warehouses")
@RequiredArgsConstructor
@Slf4j
@Validated
public class WarehouseController {
    private final WarehouseService warehouseService;

    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseResponse>> createWarehouse(
            @Valid @RequestBody CreateWarehouseRequest createWarehouseRequest){
        WarehouseResponse  warehouseResponse = warehouseService.createWarehouse(createWarehouseRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Warehouse created successfully", warehouseResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseResponse>> getWarehouseById(@PathVariable Long id) {

        WarehouseResponse response = warehouseService.getWarehouseById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<WarehouseResponse>>> getAllWarehouses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        PageResponse<WarehouseResponse> response = warehouseService.getAllWarehouse(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWarehouse(@PathVariable Long id) {

        warehouseService.deleteWarehouseById(id);
        return ResponseEntity.ok(ApiResponse.success("Warehouse deleted successfully", null));
    }

    @GetMapping("/{warehouseId}/stocks")
    public ResponseEntity<ApiResponse<PageResponse<WarehouseStockResponse>>> getAllStockInWarehouse(
            @PathVariable Long warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        PageResponse<WarehouseStockResponse> response = warehouseService.getAllStockInWarehouse(warehouseId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{warehouseId}/stocks/product/{productId}")
    public ResponseEntity<ApiResponse<WarehouseStockResponse>> getStockByproductId(
            @PathVariable Long warehouseId, @PathVariable Long productId){

        WarehouseStockResponse response = warehouseService.getStockByProductId(warehouseId, productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{warehouseId}/stocks/product/{productId}")
    public ResponseEntity<ApiResponse<WarehouseStockResponse>> updateStock(
            @PathVariable Long warehouseId,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateStockRequest updateStockRequest) {

        WarehouseStockResponse response = warehouseService.updateStock(warehouseId, productId, updateStockRequest);
        return ResponseEntity.ok(ApiResponse.success("Stock updated successfully", response));
    }

    @GetMapping("/{warehouseId}/stocks/low-stock")
    public ResponseEntity<ApiResponse<List<WarehouseStockResponse>>> getLowStockProducts(
            @PathVariable Long warehouseId) {

        List<WarehouseStockResponse> response = warehouseService.getLowStockProducts(warehouseId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{warehouseId}/stocks/out-of-stock")
    public ResponseEntity<ApiResponse<List<WarehouseStockResponse>>> getOutOfStockProducts(
            @PathVariable Long warehouseId) {

        List<WarehouseStockResponse> response = warehouseService.getOutOfStockProducts(warehouseId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{warehouseId}/inventory/total")
    public ResponseEntity<ApiResponse<Long>> getTotalInventoryCount(
            @PathVariable Long warehouseId) {

        Long total = warehouseService.getTotalInventoryCount(warehouseId);
        return ResponseEntity.ok(ApiResponse.success("Total inventory count retrieved", total));
    }
}