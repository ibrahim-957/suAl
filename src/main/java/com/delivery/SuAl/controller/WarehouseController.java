package com.delivery.SuAl.controller;

import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.model.request.warehouse.CollectContainersRequest;
import com.delivery.SuAl.model.request.warehouse.CreateWarehouseRequest;
import com.delivery.SuAl.model.request.warehouse.UpdateStockRequest;
import com.delivery.SuAl.model.response.warehouse.ContainerCollectionResponse;
import com.delivery.SuAl.model.response.warehouse.WarehouseResponse;
import com.delivery.SuAl.model.response.warehouse.WarehouseStockResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.service.ContainerCollectionService;
import com.delivery.SuAl.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/v1/api/warehouses")
@RequiredArgsConstructor
@Slf4j
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class WarehouseController {
    private final WarehouseService warehouseService;
    private final ContainerCollectionService containerCollectionService;

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

    @PostMapping("/collect-containers")
    public ResponseEntity<ApiResponse<ContainerCollectionResponse>> collectContainers(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CollectContainersRequest request
    ) {
        log.info("Container collection request from admin: {} for warehouse: {}",
                user.getEmail(), request.getWarehouseId());

        ContainerCollectionResponse response = containerCollectionService
                .collectContainers(request, user.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Containers collected successfully", response));
    }


    @GetMapping("/{warehouseId}/collections")
    public ResponseEntity<ApiResponse<PageResponse<ContainerCollectionResponse>>> getCollectionsByWarehouse(
            @PathVariable Long warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "collectionDateTime") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {

        log.info("GET /v1/api/warehouses/{}/collections - Fetching collections", warehouseId);

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, sortDirection, sortBy);

        Page<ContainerCollectionResponse> collections = containerCollectionService
                .getCollectionsByWarehouse(warehouseId, pageable);

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(collections.getContent(), collections)));
    }

    @GetMapping("/collections/date-range")
    public ResponseEntity<ApiResponse<List<ContainerCollectionResponse>>> getCollectionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        log.info("GET /v1/api/warehouses/collections/date-range - From {} to {}", startDate, endDate);

        List<ContainerCollectionResponse> collections = containerCollectionService
                .getCollectionsByDateRange(startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success("Collections retrieved successfully", collections));
    }

    @GetMapping("/{warehouseId}/products/{productId}/containers/empty/total")
    public ResponseEntity<ApiResponse<Integer>> getTotalEmptyContainers(
            @PathVariable Long warehouseId,
            @PathVariable Long productId
    ) {
        log.info("GET /v1/api/warehouses/{}/products/{}/containers/empty/total", warehouseId, productId);

        Integer total = containerCollectionService
                .getTotalEmptyContainers(warehouseId, productId);

        return ResponseEntity.ok(ApiResponse.success("Total empty containers retrieved", total));
    }

    @GetMapping("/{warehouseId}/products/{productId}/containers/damaged/total")
    public ResponseEntity<ApiResponse<Integer>> getTotalDamagedContainers(
            @PathVariable Long warehouseId,
            @PathVariable Long productId
    ) {
        log.info("GET /v1/api/warehouses/{}/products/{}/containers/damaged/total", warehouseId, productId);

        Integer total = containerCollectionService
                .getTotalDamagedContainers(warehouseId, productId);

        return ResponseEntity.ok(ApiResponse.success("Total damaged containers retrieved", total));
    }

    @GetMapping("/collections/{collectionId}")
    public ResponseEntity<ApiResponse<ContainerCollectionResponse>> getCollectionById(
            @PathVariable Long collectionId
    ) {
        log.info("GET /v1/api/warehouses/collections/{} - Fetching collection details", collectionId);

        ContainerCollectionResponse response = containerCollectionService
                .getCollectionById(collectionId);

        return ResponseEntity.ok(ApiResponse.success("Collection details retrieved", response));
    }
}