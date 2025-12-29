package com.delivery.SuAl.model.request.marketing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApplyPromoRequest {
    @NotBlank
    private String promoCode;

    private BigDecimal orderAmount;

    @NotNull
    private Long userId;

    private Long orderId;
}