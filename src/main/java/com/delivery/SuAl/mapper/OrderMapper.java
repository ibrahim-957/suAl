package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Customer;
import com.delivery.SuAl.entity.Driver;
import com.delivery.SuAl.entity.Operator;
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
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {OrderDetailMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface OrderMapper {
    Order toEntity(CreateOrderByOperatorRequest createOrderRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "operator", ignore = true)
    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "promo", ignore = true)
    @Mapping(target = "orderDetails", ignore = true)
    @Mapping(target = "campaignBonuses", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdateOrderRequest updateOrderRequest,
                                 @MappingTarget Order order);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", expression = "java(getFullName(order.getCustomer()))")
    @Mapping(target = "phoneNumber", source = "customer.user.phoneNumber")
    @Mapping(target = "operatorId", source = "operator.id")
    @Mapping(target = "operatorName", expression = "java(getFullName(order.getOperator()))")
    @Mapping(target = "driverId", source = "driver.id")
    @Mapping(target = "driverName", expression = "java(getFullName(order.getDriver()))")
    @Mapping(target = "promoCode", source = "promo.promoCode")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "totalAmount", source = "amount")
    @Mapping(target = "finalAmount", source = "totalAmount")
    @Mapping(target = "campaignBonuses", expression = "java(mapCampaignBonuses(order.getCampaignBonuses()))")
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

    default String getFullName(Customer customer) {
        if (customer == null) return "";
        return (customer.getFirstName() != null ? customer.getFirstName() : "") +
                " " +
                (customer.getLastName() != null ? customer.getLastName() : "");
    }

    default String getFullName(Driver driver) {
        if (driver == null) return "";
        return (driver.getFirstName() != null ? driver.getFirstName() : "") +
                " " +
                (driver.getLastName() != null ? driver.getLastName() : "");
    }

    default String getFullName(Operator operator) {
        if (operator == null) return "";
        return (operator.getFirstName() != null ? operator.getFirstName() : "") +
                " " +
                (operator.getLastName() != null ? operator.getLastName() : "");
    }
}