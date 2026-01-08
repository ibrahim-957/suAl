package com.delivery.SuAl.model.response.order;

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
public class OrderCampaignBonusResponse {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private String campaignCode;
    private String campaignName;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal originalValue;
    private LocalDateTime createdAt;
}
