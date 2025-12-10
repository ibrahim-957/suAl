package com.delivery.SuAl.model.request.marketing;

import com.delivery.SuAl.model.CampaignStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCampaignRequest {
    private String description;

    private Long buyProductId;

    @Min(value = 1)
    @Max(value = 100)
    private Integer buyQuantity;

    private Long freeProductId;

    @Min(value = 1)
    @Max(value = 100)
    private Integer freeQuantity;

    private CampaignStatus  campaignStatus;

    private LocalDate validFrom;

    private LocalDate validTo;
}
