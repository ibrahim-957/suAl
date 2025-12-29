package com.delivery.SuAl.model.response.statisticsAnddashborad;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CampaignUsageStatsResponse {
    private Long totalUsages;
    private Long uniqueUsers;
    private Integer totalFreeProductsGiven;
    private Integer remainingUses;
}
