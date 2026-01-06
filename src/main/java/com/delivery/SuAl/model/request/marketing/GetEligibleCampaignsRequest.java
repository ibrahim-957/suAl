package com.delivery.SuAl.model.request.marketing;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetEligibleCampaignsRequest {
    @NotNull
    private Long userId;

    @NotNull
    private Map<Long, Integer> productQuantities;

    private Boolean willUsePromoCode;
}