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
public class ApplyPromoResponse {
    private Boolean success;
    private String message;
    private Long promoUsageId;
    private BigDecimal discountApplied;
    private BigDecimal originalAmount;
    private BigDecimal finalAmount;
    private String promoCode;
}
