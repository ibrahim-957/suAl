package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.CampaignUsage;
import com.delivery.SuAl.model.response.marketing.CampaignUsageResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CampaignUsageMapper {
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.firstName", target = "userName")
    @Mapping(source = "campaign.id", target = "campaignId")
    @Mapping(source = "campaign.campaignId", target = "campaignCode")
    @Mapping(source = "campaign.name", target = "campaignName")
    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "order.orderNumber", target = "orderNumber")
    CampaignUsageResponse toResponse(CampaignUsage campaignUsage);
}
