package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.warehouse.UpdateStockRequest;
import com.delivery.SuAl.model.response.warehouse.WarehouseStockResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WarehouseService {
    PageResponse<WarehouseStockResponse> getAllWarehouses(Pageable pageable);

    WarehouseStockResponse getWarehouseByProductId(Long productId);

    WarehouseStockResponse updateStock(Long id, UpdateStockRequest updateStockRequest);

    List<WarehouseStockResponse> getLowStockProducts();

    List<WarehouseStockResponse> getOutOfStockProducts();

    Long getTotalInventoryCount();
}
