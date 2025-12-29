package com.delivery.SuAl.model.request.marketing;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCampaignRequest {
    @NotBlank(message = "Campaign ID is required")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Campaign ID must contain only uppercase letters, numbers, and underscores")
    @Size(min = 4, max = 30, message = "Campaign ID must be between 4 and 30 characters")
    private String campaignId;

    @NotBlank(message = "Campaign name is required")
    @Size(min = 3, max = 100, message = "Campaign name must be between 3 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Buy product ID is required")
    private Long buyProductId;

    @Min(value = 1, message = "Buy quantity must be at least 1")
    private int buyQuantity;

    @NotNull(message = "Free product ID is required")
    private Long freeProductId;

    @Min(value = 1, message = "Free quantity must be at least 1")
    private int freeQuantity;

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
