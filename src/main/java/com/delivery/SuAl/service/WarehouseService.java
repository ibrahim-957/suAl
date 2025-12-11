package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.warehouse.CreateWarehouseRequest;
import com.delivery.SuAl.model.request.warehouse.UpdateStockRequest;
import com.delivery.SuAl.model.response.warehouse.WarehouseResponse;
import com.delivery.SuAl.model.response.warehouse.WarehouseStockResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WarehouseService {

    WarehouseResponse createWarehouse(CreateWarehouseRequest createWarehouseRequest);

    WarehouseResponse getWarehouseById(Long id);

    PageResponse<WarehouseResponse> getAllWarehouse(Pageable pageable);

    void deleteWarehouseById(Long id);

    PageResponse<WarehouseStockResponse> getAllStockInWarehouse(Long warehouseId, Pageable pageable);

    WarehouseStockResponse getStockByProductId(Long warehouseId, Long productId);

    WarehouseStockResponse updateStock(Long warehouseId, Long productId, UpdateStockRequest updateStockRequest);

    List<WarehouseStockResponse> getLowStockProducts(Long warehouseId);

    List<WarehouseStockResponse> getOutOfStockProducts(Long warehouseId);

    Long getTotalInventoryCount(Long warehouseId);
}
