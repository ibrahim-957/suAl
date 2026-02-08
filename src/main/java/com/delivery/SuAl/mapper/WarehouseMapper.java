package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Warehouse;
import com.delivery.SuAl.model.request.warehouse.CreateWarehouseRequest;
import com.delivery.SuAl.model.request.warehouse.UpdateStockRequest;
import com.delivery.SuAl.model.response.warehouse.WarehouseResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", uses = {DateTimeMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface WarehouseMapper {
    Warehouse toEntity(CreateWarehouseRequest createWarehouseRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateStockFromRequest(UpdateStockRequest updateStockRequest,
                                @MappingTarget Warehouse warehouse);

    @Mapping(target = "createdAt", qualifiedByName = "utcToBaku")
    @Mapping(target = "updatedAt", qualifiedByName = "utcToBaku")
    WarehouseResponse toResponse(Warehouse warehouse);

    List<WarehouseResponse> toResponseList(List<Warehouse> warehouses);

}
