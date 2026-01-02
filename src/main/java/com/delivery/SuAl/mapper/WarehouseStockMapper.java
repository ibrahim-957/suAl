package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.WarehouseStock;
import com.delivery.SuAl.model.response.warehouse.WarehouseStockResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WarehouseStockMapper {
    @Mapping(target = "totalCount", expression = "java(calculateTotalCount(warehouseStock))")
    @Mapping(target = "lowStock", expression = "java(isLowStock(warehouseStock))")
    @Mapping(target = "outOfStock", expression = "java(isOutOfStock(warehouseStock))")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "size", source = "product.size")
    @Mapping(target = "companyName", source = "product.company.name")
    WarehouseStockResponse toResponse(WarehouseStock warehouseStock);

    List<WarehouseStockResponse> toResponseList(List<WarehouseStock> warehouseStocks);

    default Integer calculateTotalCount(WarehouseStock warehouseStock) {
        return warehouseStock.getFullCount() +
                warehouseStock.getEmptyCount() +
                warehouseStock.getDamagedCount();
    }

    default boolean isLowStock(WarehouseStock warehouseStock) {
        return warehouseStock.getFullCount() < warehouseStock.getMinimumStockAlert();
    }

    default boolean isOutOfStock(WarehouseStock warehouseStock) {
        return warehouseStock.getFullCount() == 0;
    }
}
