package com.delivery.SuAl.model.request.marketing;

import com.delivery.SuAl.model.CampaignStatus;
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

    @Min(value = 1)
    private Integer buyQuantity;

    @Min(value = 1)
    private Integer freeQuantity;

    private Integer maxUsesPerUser;

    private Integer maxTotalUses;

    private CampaignStatus  campaignStatus;

    private LocalDate validFrom;

    private LocalDate validTo;
}
