package com.delivery.SuAl.model.response.marketing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ValidateCampaignResponse {
    private Boolean isValid;
    private String message;
    private CampaignResponse campaignResponse;
    private Integer freeQuantity;
    private Long freeProductId;
    private String freeProductName;
    private BigDecimal estimatedBonusValue;
    private Boolean userCanUse;
    private Integer userUsageCount;
}
