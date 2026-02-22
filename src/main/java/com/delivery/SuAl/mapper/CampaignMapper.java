package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Campaign;
import com.delivery.SuAl.model.request.marketing.CreateCampaignRequest;
import com.delivery.SuAl.model.request.marketing.UpdateCampaignRequest;
import com.delivery.SuAl.model.response.marketing.CampaignResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = {DateTimeMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CampaignMapper {
    Campaign toEntity(CreateCampaignRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "buyProduct", ignore = true)
    @Mapping(target = "freeProduct", ignore = true)
    @Mapping(target = "currentTotalUses", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdateCampaignRequest request, @MappingTarget Campaign campaign);

    @Mapping(target = "buyProductId", expression = "java(getBuyProductId(campaign))")
    @Mapping(target = "buyProductName", expression = "java(getBuyProductName(campaign))")
    @Mapping(target = "freeProductId", expression = "java(getFreeProductId(campaign))")
    @Mapping(target = "freeProductName", expression = "java(getFreeProductName(campaign))")
    @Mapping(target = "campaignTypeDisplay", source = "campaignType")
    @Mapping(target = "usageRemaining", expression = "java(calculateUsageRemaining(campaign))")
    @Mapping(target = "isCurrentlyActive", expression = "java(campaign.isActive())")
    @Mapping(target = "maxUsesPerCustomer", source = "maxUsesPerCustomer")
    @Mapping(target = "createdAt", qualifiedByName = "utcToBaku")
    @Mapping(target = "updatedAt", qualifiedByName = "utcToBaku")
    CampaignResponse toResponse(Campaign campaign);

    default Long getBuyProductId(Campaign campaign) {
        if (campaign.getBuyProduct() == null) {
            return null;
        }
        return campaign.getBuyProduct().getId();
    }

    default String getBuyProductName(Campaign campaign) {
        if (campaign.getBuyProduct() == null) {
            return null;
        }
        return campaign.getBuyProduct().getName();
    }

    default Long getFreeProductId(Campaign campaign) {
        if (campaign.getFreeProduct() == null) {
            return null;
        }
        return campaign.getFreeProduct().getId();
    }

    default String getFreeProductName(Campaign campaign) {
        if (campaign.getFreeProduct() == null) {
            return null;
        }
        return campaign.getFreeProduct().getName();
    }

    default Integer calculateUsageRemaining(Campaign campaign) {
        if (campaign.getMaxTotalUses() == null) {
            return null;
        }
        return Math.max(0, campaign.getMaxTotalUses() - campaign.getCurrentTotalUses());
    }
}