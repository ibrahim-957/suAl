package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.WarehouseTransferItem;
import com.delivery.SuAl.model.request.transfer.WarehouseTransferItemRequest;
import com.delivery.SuAl.model.response.transfer.WarehouseTransferItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface WarehouseTransferItemMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "transfer", ignore = true)
    WarehouseTransferItem toEntity(WarehouseTransferItemRequest request);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productSize", source = "product.size.label")
    @Mapping(target = "companyName", source = "product.company.name")
    WarehouseTransferItemResponse toResponse(WarehouseTransferItem item);

    List<WarehouseTransferItemResponse> toResponseList(List<WarehouseTransferItem> items);
}
