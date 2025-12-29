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
public class ApplyCampaignResponse {
    private Boolean success;
    private String message;
    private Long campaignUsageId;
    private String campaignId;
    private String campaignName;

    private Long freeProductId;
    private String freeProductName;
    private Integer freeQuantity;
    private BigDecimal bonusValue;
}
