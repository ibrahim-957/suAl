package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.model.request.order.CreateOrderRequest;
import com.delivery.SuAl.model.request.order.UpdateOrderRequest;
import com.delivery.SuAl.model.response.order.OrderResponse;
import com.delivery.SuAl.model.response.order.OrderSummaryResponse;
import com.delivery.SuAl.model.response.search.OrderSearchResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

import java.util.List;
@Mapper(componentModel = "spring", uses = {OrderDetailMapper.class})
public interface OrderMapper {
    Order toEntity(CreateOrderRequest createOrderRequest);

    void updateEntityFromRequest(UpdateOrderRequest updateOrderRequest,
                                 @MappingTarget Order order);

    @Mappings({
            @Mapping(target = "operatorId", source = "operator.id"),
            @Mapping(target = "operatorName", source = "operator.firstName"),

            @Mapping(target = "driverId", source = "driver.id"),
            @Mapping(target = "driverName", source = "driver.firstName"),

            @Mapping(target = "promoCode", source = "promo.promoCode"),

            @Mapping(target = "address", source = "address")
    })
    OrderResponse toResponse(Order order);

    OrderSummaryResponse toSummaryResponse(Order order);

    OrderSearchResponse toSearchResponse(Order order);

    List<OrderResponse> toResponseList(List<Order> orders);

    List<OrderSummaryResponse> toSummaryResponseList(List<Order> orders);

    List<OrderSearchResponse> toSearchResponseList(List<Order> orders);
}
