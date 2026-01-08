package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.OrderCampaignBonus;
import com.delivery.SuAl.model.request.order.CreateOrderByOperatorRequest;
import com.delivery.SuAl.model.request.order.UpdateOrderRequest;
import com.delivery.SuAl.model.response.order.OrderCampaignBonusResponse;
import com.delivery.SuAl.model.response.order.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {OrderDetailMapper.class})
public interface OrderMapper {
    Order toEntity(CreateOrderByOperatorRequest createOrderRequest);

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

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderNumber", source = "order.orderNumber")
    @Mapping(target = "campaignCode", source = "campaign.campaignCode")
    @Mapping(target = "campaignName", source = "campaign.name")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    OrderCampaignBonusResponse toCampaignBonusResponse(OrderCampaignBonus bonus);

    default List<OrderCampaignBonusResponse> mapCampaignBonuses(List<OrderCampaignBonus> bonuses) {
        if (bonuses == null) {
            return new ArrayList<>();
        }
        return bonuses.stream()
                .map(this::toCampaignBonusResponse)
                .collect(Collectors.toList());
    }
}