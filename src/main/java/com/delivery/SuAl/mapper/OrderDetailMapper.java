package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.OrderDetail;
import com.delivery.SuAl.model.request.order.OrderItemRequest;
import com.delivery.SuAl.model.response.order.OrderDetailResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderDetailMapper {
    OrderDetail toEntity(OrderItemRequest orderItemRequest);

    OrderDetailResponse toResponse(OrderDetail orderDetail);

    List<OrderDetailResponse> toResponseList(List<OrderDetail> orderDetails);
}
