package com.delivery.SuAl.model.response.marketing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PromoUsageResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long promoId;
    private String promoCode;
    private Long orderId;
    private String orderNumber;
    private Long campaignId;
    private BigDecimal discountApplied;
    private BigDecimal orderAmount;
    private LocalDateTime usedAt;
}
