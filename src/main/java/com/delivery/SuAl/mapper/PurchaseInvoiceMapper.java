package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.PurchaseInvoice;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.model.request.purchase.CreatePurchaseInvoiceRequest;
import com.delivery.SuAl.model.request.purchase.UpdatePurchaseInvoiceRequest;
import com.delivery.SuAl.model.response.purchase.PurchaseInvoiceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {DateTimeMapper.class, PurchaseInvoiceItemMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PurchaseInvoiceMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "warehouse", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PurchaseInvoice toEntity(CreatePurchaseInvoiceRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "warehouse", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdatePurchaseInvoiceRequest request,
                                 @MappingTarget PurchaseInvoice invoice);

    @Mapping(target = "CompanyId", source = "company.id")
    @Mapping(target = "CompanyName", source = "company.name")
    @Mapping(target = "warehouseId", source = "warehouse.id")
    @Mapping(target = "warehouseName", source = "warehouse.name")
    @Mapping(target = "approvedBy", expression = "java(getUserIdentifier(invoice.getApprovedBy()))")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "approvedAt", qualifiedByName = "utcToBaku")
    @Mapping(target = "createdAt", qualifiedByName = "utcToBaku")
    @Mapping(target = "updatedAt", qualifiedByName = "utcToBaku")
    PurchaseInvoiceResponse toResponse(PurchaseInvoice invoice);

    List<PurchaseInvoiceResponse> toResponseList(List<PurchaseInvoice> invoices);

    default String getUserIdentifier(User user) {
        if (user == null) return null;
        return user.getEmail() != null ? user.getEmail() : user.getPhoneNumber();
    }
}
