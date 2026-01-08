package com.delivery.SuAl.model.request.marketing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ValidatePromoRequest {
    private String promoCode;
    private BigDecimal orderAmount;
    private Long userId;
}
