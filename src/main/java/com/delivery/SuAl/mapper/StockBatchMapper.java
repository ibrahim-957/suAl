package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.StockBatch;
import com.delivery.SuAl.model.response.inventory.StockBatchResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", uses = {DateTimeMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StockBatchMapper {
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "warehouseId", source = "warehouse.id")
    @Mapping(target = "warehouseName", source = "warehouse.name")
    @Mapping(target = "purchaseInvoiceItemId", source = "invoiceItem.id")
    @Mapping(target = "invoiceNumber", source = "invoiceItem.invoice.invoiceNumber")
    @Mapping(target = "consumedQuantity", expression = "java(calculateConsumed(batch))")
    @Mapping(target = "createdAt", qualifiedByName = "utcToBaku")
    @Mapping(target = "salePrice", source = "invoiceItem.salePrice")
    StockBatchResponse toResponse(StockBatch batch);

    List<StockBatchResponse> toResponseList(List<StockBatch> batches);

    default Integer calculateConsumed(StockBatch batch) {
        if (batch.getInitialQuantity() == null || batch.getRemainingQuantity() == null) return 0;
        return batch.getInitialQuantity() - batch.getRemainingQuantity();
    }
}
