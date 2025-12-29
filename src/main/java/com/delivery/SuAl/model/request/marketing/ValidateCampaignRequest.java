package com.delivery.SuAl.model.request.marketing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidateCampaignRequest {
    @NotBlank
    private String campaignId;

    @NotNull
    private Long userId;

    @NotNull
    private Long buyProductId;

    @NotNull
    private Integer buyQuantity;
}
