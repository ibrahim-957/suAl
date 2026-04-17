package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.PurchaseInvoiceItem;
import com.delivery.SuAl.model.request.purchase.PurchaseInvoiceItemRequest;
import com.delivery.SuAl.model.response.purchase.PurchaseInvoiceItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PurchaseInvoiceItemMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "lineTotal", ignore = true)
    PurchaseInvoiceItem toEntity(PurchaseInvoiceItemRequest request);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productSize", source = "product.size.label")
    @Mapping(target = "companyName", source = "product.company.name")
    @Mapping(target = "lineTotal", expression = "java(calculateLineTotal(item))")
    PurchaseInvoiceItemResponse toResponse(PurchaseInvoiceItem item);

    List<PurchaseInvoiceItemResponse> toResponseList(List<PurchaseInvoiceItem> items);

    default BigDecimal calculateLineTotal(PurchaseInvoiceItem item) {
        if (item.getLineTotal() != null) return item.getLineTotal();
        if (item.getPurchasePrice() == null || item.getQuantity() == null) return BigDecimal.ZERO;
        return item.getPurchasePrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}
