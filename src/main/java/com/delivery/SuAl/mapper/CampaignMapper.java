package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Campaign;
import com.delivery.SuAl.model.request.marketing.CreateCampaignRequest;
import com.delivery.SuAl.model.request.marketing.UpdateCampaignRequest;
import com.delivery.SuAl.model.response.marketing.CampaignResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CampaignMapper {
    Campaign toEntity(CreateCampaignRequest request);

    void updateEntityFromRequest(UpdateCampaignRequest request, @MappingTarget Campaign campaign);

    @Mapping(source = "buyProduct.id", target = "buyProductId")
    @Mapping(source = "buyProduct.name", target = "buyProductName")
    @Mapping(source = "freeProduct.id", target = "freeProductId")
    @Mapping(source = "freeProduct.name", target = "freeProductName")
    @Mapping(target = "usageRemaining", expression = "java(calculateUsageRemaining(campaign))")
    @Mapping(target = "isCurrentlyActive", expression = "java(isCurrentlyActive(campaign))")
    CampaignResponse toResponse(Campaign campaign);

    default Integer calculateUsageRemaining(Campaign campaign){
        if (campaign.getMaxTotalUses() == null)
            return null;
        return Math.max(0, campaign.getMaxTotalUses() - campaign.getCurrentTotalUses());
    }

    default Boolean isCurrentlyActive(Campaign campaign){
        return campaign.isActive();
    }
}
