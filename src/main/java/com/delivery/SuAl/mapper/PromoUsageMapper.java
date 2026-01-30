package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Customer;
import com.delivery.SuAl.entity.PromoUsage;
import com.delivery.SuAl.model.response.marketing.PromoUsageResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PromoUsageMapper {

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", expression = "java(getCustomerFullName(promoUsage.getCustomer()))")
    @Mapping(target = "promoId", source = "promo.id")
    @Mapping(target = "promoCode", source = "promo.promoCode")
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderNumber", source = "order.orderNumber")
    @Mapping(target = "orderAmount", source = "order.totalAmount")
    PromoUsageResponse toResponse(PromoUsage promoUsage);

    List<PromoUsageResponse> toResponseList(List<PromoUsage> promoUsages);

    default String getCustomerFullName(Customer customer) {
        if (customer == null) return "";
        return (customer.getFirstName() != null ? customer.getFirstName() : "") +
                " " +
                (customer.getLastName() != null ? customer.getLastName() : "");
    }
}
