package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.model.request.order.CreateOrderRequest;
import com.delivery.SuAl.model.request.order.UpdateOrderRequest;
import com.delivery.SuAl.model.response.order.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {OrderDetailMapper.class})
public interface OrderMapper {
    Order toEntity(CreateOrderRequest createOrderRequest);

    void updateEntityFromRequest(UpdateOrderRequest updateOrderRequest,
                                 @MappingTarget Order order);

    @Mappings({
            @Mapping(target = "customerName", source = "user.firstName"),
            @Mapping(target = "phoneNumber", source = "user.phoneNumber"),
            @Mapping(target = "operatorId", source = "operator.id"),
            @Mapping(target = "operatorName", source = "operator.firstName"),

            @Mapping(target = "driverId", source = "driver.id"),
            @Mapping(target = "driverName", source = "driver.firstName"),

            @Mapping(target = "promoCode", source = "promo.promoCode"),

            @Mapping(target = "address", source = "address"),
            @Mapping(target = "totalAmount", source = "amount"),
            @Mapping(target = "finalAmount", source = "totalAmount")
    })
    OrderResponse toResponse(Order order);
}