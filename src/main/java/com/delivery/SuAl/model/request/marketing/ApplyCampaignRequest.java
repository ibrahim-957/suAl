package com.delivery.SuAl.model.request.marketing;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApplyCampaignRequest {
    @NotBlank
    private String campaignCode;
    @NotNull
    private Long userId;

    @NotNull
    private Long orderId;
}