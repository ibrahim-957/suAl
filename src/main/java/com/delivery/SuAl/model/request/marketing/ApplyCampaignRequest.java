package com.delivery.SuAl.model.request.marketing;

import jakarta.validation.constraints.Min;
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
    private String campaignId;

    @NotNull
    private Long userId;

    @NotNull
    private Long buyProductId;

    @NotNull
    @Min(value = 1)
    private Integer buyQuantity;

    private Long orderId;
}
