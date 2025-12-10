package com.delivery.SuAl.model.request.marketing;

import jakarta.validation.constraints.Max;
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
    @Size(min = 3, max = 100, message = "Campaign ID must be between 3 and 100 characters")
    @Pattern(
            regexp = "^[A-Z0-9_]+$",
            message = "Campaign ID must contain only uppercase letters, numbers, and underscores"
    )
    private String campaignId;

    private String description;

    @NotNull
    private Long buyProductId;

    @NotNull
    @Min(value = 1)
    @Max(value = 100)
    private Integer buyQuantity;

    @NotNull
    private Integer freeProductId;

    @NotNull
    @Min(value = 1)
    @Max(value = 100)
    private Integer freeQuantity;

    private LocalDate validFrom;

    private LocalDate validTo;
}
