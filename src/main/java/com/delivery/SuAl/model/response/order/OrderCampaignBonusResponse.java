package com.delivery.SuAl.model.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderCampaignBonusResponse {
    private Long id;
    private String campaignId;
    private String productName;
    private Integer quantity;
    private BigDecimal originalValue;
}
