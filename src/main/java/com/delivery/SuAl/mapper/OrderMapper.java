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

import java.util.List;
@Mapper(componentModel = "spring")
public interface OrderMapper {
    Order toEntity(CreateOrderRequest createOrderRequest);

    void updateEntityFromRequest(UpdateOrderRequest updateOrderRequest,
                                 @MappingTarget Order order);

    @Mapping(target = "totalItems", source = "count")
    @Mapping(target = "finalAmount", source = "amount")
    OrderResponse toResponse(Order order);

    OrderSummaryResponse toSummaryResponse(Order order);

    OrderSearchResponse toSearchResponse(Order order);

    List<OrderResponse> toResponseList(List<Order> orders);

    List<OrderSummaryResponse> toSummaryResponseList(List<Order> orders);

    List<OrderSearchResponse> toSearchResponseList(List<Order> orders);

    default String getOperatorName(Order order) {
        if (order.getOperator() == null) return null;
        return order.getOperator().getFirstName() + " " + order.getOperator().getLastName();
    }

    default String getDriverName(Order order) {
        if (order.getDriver() == null) return null;
        return order.getDriver().getFirstName() + " " + order.getDriver().getLastName();
    }
}
