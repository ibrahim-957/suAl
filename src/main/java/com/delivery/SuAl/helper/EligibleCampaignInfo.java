package com.delivery.SuAl.helper;

import com.delivery.SuAl.model.CampaignType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EligibleCampaignInfo {
    private String campaignCode;
    private String campaignName;
    private String description;
    private CampaignType campaignType;

    private Long buyProductId;
    private String buyProductName;
    private Integer buyQuantityRequired;
    private Integer buyQuantityInBasket;

    private Long freeProductId;
    private String freeProductName;
    private Integer freeQuantity;
    private BigDecimal freeProductPrice;
    private BigDecimal bonusValue;

    private Boolean freeProductHasDeposit;
    private BigDecimal depositPerUnit;
    private BigDecimal totalDepositForFree;

    private Boolean willBeApplied;
    private String notAppliedReason;
}
