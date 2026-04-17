package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.WarehouseTransfer;
import com.delivery.SuAl.model.request.transfer.CreateWarehouseTransferRequest;
import com.delivery.SuAl.model.request.transfer.UpdateWarehouseTransferRequest;
import com.delivery.SuAl.model.response.transfer.WarehouseTransferResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {DateTimeMapper.class, WarehouseTransferItemMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface WarehouseTransferMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fromWarehouse", ignore = true)
    @Mapping(target = "toWarehouse", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    WarehouseTransfer toEntity(CreateWarehouseTransferRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fromWarehouse", ignore = true)
    @Mapping(target = "toWarehouse", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdateWarehouseTransferRequest request,
                                 @MappingTarget WarehouseTransfer transfer);

    @Mapping(target = "fromWarehouseId", source = "fromWarehouse.id")
    @Mapping(target = "fromWarehouseName", source = "fromWarehouse.name")
    @Mapping(target = "toWarehouseId", source = "toWarehouse.id")
    @Mapping(target = "toWarehouseName", source = "toWarehouse.name")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "completedAt", qualifiedByName = "utcToBaku")
    @Mapping(target = "createdAt", qualifiedByName = "utcToBaku")
    @Mapping(target = "updatedAt", qualifiedByName = "utcToBaku")
    WarehouseTransferResponse toResponse(WarehouseTransfer transfer);

    List<WarehouseTransferResponse> toResponseList(List<WarehouseTransfer> transfers);
}
