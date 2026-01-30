package com.delivery.SuAl.model.request.marketing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidateCampaignRequest {
    @NotBlank
    private String campaignCode;

    @NotNull
    private Long customerId;

    @NotNull
    private Map<Long, Integer> productQuantities;

    private BigDecimal orderTotal;
}