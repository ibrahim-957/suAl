package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.PromoUsage;
import com.delivery.SuAl.model.response.marketing.PromoUsageResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PromoUsageMapper {
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.firstName")
    @Mapping(target = "promoId", source = "promo.id")
    @Mapping(target = "promoCode", source = "promo.promoCode")
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderNumber", source = "order.orderNumber")
    @Mapping(target = "orderAmount", source = "order.totalAmount")
    PromoUsageResponse toResponse(PromoUsage promoUsage);
}
