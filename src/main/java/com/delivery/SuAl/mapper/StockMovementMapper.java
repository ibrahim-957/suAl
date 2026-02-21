package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.StockMovement;
import com.delivery.SuAl.model.response.inventory.StockMovementResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", uses = {DateTimeMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StockMovementMapper {
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "warehouseId", source = "warehouse.id")
    @Mapping(target = "warehouseName", source = "warehouse.name")
    @Mapping(target = "createdAt", qualifiedByName = "utcToBaku")
    StockMovementResponse toResponse(StockMovement movement);

    List<StockMovementResponse> toResponseList(List<StockMovement> movements);
}
