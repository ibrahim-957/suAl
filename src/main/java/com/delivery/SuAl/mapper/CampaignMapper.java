package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Campaign;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.model.CampaignType;
import com.delivery.SuAl.model.request.marketing.CreateCampaignRequest;
import com.delivery.SuAl.model.request.marketing.UpdateCampaignRequest;
import com.delivery.SuAl.model.response.marketing.CampaignResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface CampaignMapper {
    Campaign toEntity(CreateCampaignRequest request);

    void updateEntityFromRequest(UpdateCampaignRequest request, @MappingTarget Campaign campaign);

    @Mapping(source = "buyProduct.id", target = "buyProductId")
    @Mapping(source = "buyProduct.name", target = "buyProductName")
    @Mapping(source = "freeProduct.id", target = "freeProductId")
    @Mapping(source = "freeProduct.name", target = "freeProductName")
    @Mapping(target = "campaignTypeDisplay", source = "campaignType")
    @Mapping(target = "usageRemaining", expression = "java(calculateUsageRemaining(campaign))")
    @Mapping(target = "isCurrentlyActive", expression = "java(campaign.isActive())")
    CampaignResponse toResponse(Campaign campaign);

    default Integer calculateUsageRemaining(Campaign campaign){
        if (campaign.getMaxTotalUses() == null)
            return null;
        return Math.max(0, campaign.getMaxTotalUses() - campaign.getCurrentTotalUses());
    }


    default BigDecimal calculateBonusValue(Campaign campaign){
        if (campaign.getFreeProduct() == null
                || campaign.getFreeProduct().getPrices() == null
                || campaign.getFreeProduct().getPrices().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return campaign.getFreeProduct().getPrices().getLast().getSellPrice()
                .multiply(BigDecimal.valueOf(campaign.getFreeQuantity()));
    }

    default BigDecimal getDepositPerUnit(Product product){
        if (product == null || !product.getHasDeposit()){
            return BigDecimal.ZERO;
        }
        return product.getDepositAmount();
    }

    default BigDecimal calculateTotalDeposit(Campaign campaign){
        if (campaign.getFreeProduct() == null || !campaign.getFreeProduct().getHasDeposit()){
            return BigDecimal.ZERO;
        }
        return campaign.getFreeProduct().getDepositAmount().multiply(BigDecimal.valueOf(campaign.getFreeQuantity()));
    }
}