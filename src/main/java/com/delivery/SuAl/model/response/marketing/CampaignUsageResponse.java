package com.delivery.SuAl.model.response.marketing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CampaignUsageResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long campaignId;
    private String campaignCode;
    private String campaignName;
    private Long orderId;
    private String orderNumber;
    private LocalDateTime usedAt;
}