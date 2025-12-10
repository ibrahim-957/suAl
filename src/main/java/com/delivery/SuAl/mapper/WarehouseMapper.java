package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Warehouse;
import com.delivery.SuAl.model.request.warehouse.CreateWarehouseRequest;
import com.delivery.SuAl.model.request.warehouse.UpdateStockRequest;
import com.delivery.SuAl.model.response.warehouse.WarehouseStockResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WarehouseMapper {
//    @Mapping(target = "product", ignore = true)
//    @Mapping(target = "company", ignore = true)
//    @Mapping(target = "category", ignore = true)
//    @Mapping(target = "type", ignore = true)
    Warehouse toEntity(CreateWarehouseRequest createWarehouseRequest);

//    @Mapping(target = "product", ignore = true)
    void updateStockFromRequest(UpdateStockRequest updateStockRequest,
                                @MappingTarget Warehouse warehouse);

//    @Mapping(target = "productId", source = "product.id")
//    @Mapping(target = "productName", source = "product.name")
//    @Mapping(target = "companyName", source = "company.name")
//    @Mapping(target = "size", source = "product.size")
//    @Mapping(target = "totalCount", expression = "java(calculateTotalCount(warehouse))")
//    @Mapping(target = "lowStock", expression = "java(isLowStock(warehouse))")
//    @Mapping(target = "outOfStock", expression = "java(isOutOfStock(warehouse))")
    WarehouseStockResponse toResponse(Warehouse warehouse);

    List<WarehouseStockResponse> toResponseList(List<Warehouse> warehouses);

    default Integer calculateTotalCount(Warehouse warehouse) {
        return warehouse.getFullCount() +
                warehouse.getEmptyCount() +
                warehouse.getDamagedCount();
    }

    default Boolean isLowStock(Warehouse warehouse) {
        return warehouse.getFullCount() < warehouse.getMinimumStockAlert();
    }

    default Boolean isOutOfStock(Warehouse warehouse) {
        return warehouse.getFullCount() == 0;
    }
}
