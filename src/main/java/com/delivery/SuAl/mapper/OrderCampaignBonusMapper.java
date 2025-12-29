package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.OrderCampaignBonus;
import com.delivery.SuAl.model.response.order.OrderCampaignBonusResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderCampaignBonusMapper {
    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "order.orderNumber", target = "orderNumber")
    @Mapping(source = "campaign.id", target = "campaignId")
    @Mapping(source = "campaign.campaignId", target = "campaignCode")
    @Mapping(source = "campaign.name", target = "campaignName")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    OrderCampaignBonusResponse toResponse(OrderCampaignBonus bonus);
}
