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
    private String description;
    private String buyProductName;
    private Integer buyQuantity;
    private String freeProductName;
    private Integer freeQuantity;
    private CampaignStatus campaignStatus;
    private LocalDate validFrom;
    private LocalDate validTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
