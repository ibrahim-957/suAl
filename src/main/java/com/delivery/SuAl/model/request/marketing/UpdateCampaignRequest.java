package com.delivery.SuAl.model.request.marketing;

import com.delivery.SuAl.model.enums.CampaignStatus;
import com.delivery.SuAl.model.enums.CampaignType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
    private String name;
    private String description;

    @Min(value = 1)
    private Integer buyQuantity;

    @Min(value = 1)
    private Integer freeQuantity;

    @DecimalMin(value = "0.01")
    private BigDecimal bonusAmount;

    @DecimalMin(value = "0.01")
    @DecimalMax(value = "100.00")
    private BigDecimal bonusPercentage;

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