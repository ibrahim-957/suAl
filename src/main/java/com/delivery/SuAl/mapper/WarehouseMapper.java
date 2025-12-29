package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Warehouse;
import com.delivery.SuAl.model.request.warehouse.CreateWarehouseRequest;
import com.delivery.SuAl.model.request.warehouse.UpdateStockRequest;
import com.delivery.SuAl.model.response.warehouse.WarehouseResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WarehouseMapper {
    Warehouse toEntity(CreateWarehouseRequest createWarehouseRequest);

    void updateStockFromRequest(UpdateStockRequest updateStockRequest,
                                @MappingTarget Warehouse warehouse);


    WarehouseResponse toResponse(Warehouse warehouse);

    List<WarehouseResponse> toResponseList(List<Warehouse> warehouses);

}
