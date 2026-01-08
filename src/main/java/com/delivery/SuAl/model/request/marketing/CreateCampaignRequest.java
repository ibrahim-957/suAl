package com.delivery.SuAl.model.request.marketing;

import com.delivery.SuAl.model.enums.CampaignType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCampaignRequest {
    @Size(min = 3, max = 30)
    private String campaignCode;

    @NotBlank(message = "Campaign name is required")
    @Size(min = 3, max = 100, message = "Campaign name must be between 3 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Campaign type is required")
    private CampaignType campaignType;

    @NotNull(message = "Buy product ID is required")
    private Long buyProductId;

    @Min(value = 1, message = "Buy quantity must be at least 1")
    private int buyQuantity;

    @NotNull(message = "Free product ID is required")
    private Long freeProductId;

    @Min(value = 1, message = "Free quantity must be at least 1")
    private int freeQuantity;

    private Boolean firstOrderOnly;

    @Min(0)
    private Integer minDatsSinceRegistration;

    private Boolean requiresPromoAbsence;

    @Min(value = 1, message = "Max uses per user must be at least 1")
    private Integer maxUsesPerUser;

    @Min(value = 1, message = "Max total uses must be at least 1")
    private Integer maxTotalUses;

    @NotNull(message = "Valid from date is required")
    private LocalDate validFrom;

    @NotNull(message = "Valid to date is required")
    private LocalDate validTo;

    @AssertTrue(message = "Valid from date must be before or equal to valid to date")
    private boolean isValidDateRange() {
        if (validFrom == null || validTo == null) return true;
        return validFrom.isBefore(validTo) || validFrom.isEqual(validTo);
    }
}