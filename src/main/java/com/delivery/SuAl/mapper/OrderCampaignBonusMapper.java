package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.OrderCampaignBonus;
import com.delivery.SuAl.model.response.order.OrderCampaignBonusResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface OrderCampaignBonusMapper {
    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "order.orderNumber", target = "orderNumber")
    @Mapping(source = "campaign.campaignCode", target = "campaignCode")
    @Mapping(source = "campaign.name", target = "campaignName")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    OrderCampaignBonusResponse toResponse(OrderCampaignBonus bonus);
}
