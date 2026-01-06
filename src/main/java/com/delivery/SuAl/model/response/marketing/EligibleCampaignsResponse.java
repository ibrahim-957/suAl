package com.delivery.SuAl.model.response.marketing;

import com.delivery.SuAl.helper.EligibleCampaignInfo;
import com.delivery.SuAl.helper.FreeProductSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EligibleCampaignsResponse {
    private List<EligibleCampaignInfo> eligibleCampaigns;
    private BigDecimal totalCampaignDiscount;
    private List<FreeProductSummary> allFreeProducts;
}