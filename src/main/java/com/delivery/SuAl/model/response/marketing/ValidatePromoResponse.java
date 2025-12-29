package com.delivery.SuAl.model.response.marketing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ValidatePromoResponse {
    private Boolean isValid;
    private String message;
    private PromoResponse promoResponse;
    private BigDecimal estimatedDiscount;
    private Boolean userCanUse;
    private Integer userUsageCount;
}
