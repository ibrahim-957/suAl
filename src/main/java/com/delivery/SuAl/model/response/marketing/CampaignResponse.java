package com.delivery.SuAl.model.response.marketing;

import com.delivery.SuAl.model.CampaignStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CampaignResponse {
    private Long id;
    private String campaignId;
    private String name;
    private String description;

    private Long buyProductId;
    private String buyProductName;
    private Integer buyQuantity;

    private Long freeProductId;
    private String freeProductName;
    private Integer freeQuantity;

    private Integer maxUsesPerUser;
    private Integer maxTotalUses;
    private Integer currentTotalUses;
    private Integer usageRemaining;

    private boolean isCurrentlyActive;
    private CampaignStatus campaignStatus;
    private LocalDate validFrom;
    private LocalDate validTo;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}