package com.delivery.SuAl.model.response.marketing;

import com.delivery.SuAl.model.enums.CampaignStatus;
import com.delivery.SuAl.model.enums.CampaignType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CampaignResponse {
    private Long id;
    private String campaignCode;
    private String name;
    private String description;
    private String imageUrl;

    private CampaignType campaignType;
    private String campaignTypeDisplay;

    private Long buyProductId;
    private String buyProductName;
    private Integer buyQuantity;

    private Long freeProductId;
    private String freeProductName;
    private Integer freeQuantity;

    private BigDecimal bonusAmount;
    private BigDecimal bonusPercentage;

    private Boolean firstOrderOnly;
    private Integer minDaysSinceRegistration;
    private Boolean requiresPromoAbsence;

    private Integer maxUsesPerCustomer;
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