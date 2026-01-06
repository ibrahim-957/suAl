package com.delivery.SuAl.model.request.marketing;

import com.delivery.SuAl.model.CampaignStatus;
import com.delivery.SuAl.model.CampaignType;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCampaignRequest {
    private String name;
    private String description;

    private CampaignType campaignType;

    @Min(value = 1)
    private Integer buyQuantity;

    @Min(value = 1)
    private Integer freeQuantity;

    private Boolean firstOrderOnly;

    @Min(value = 0)
    private Integer minDaysSinceRegistration;

    private Boolean requiresPromoAbsence;

    private Integer maxUsesPerUser;

    private Integer maxTotalUses;

    private CampaignStatus  campaignStatus;

    private LocalDate validFrom;

    private LocalDate validTo;
}