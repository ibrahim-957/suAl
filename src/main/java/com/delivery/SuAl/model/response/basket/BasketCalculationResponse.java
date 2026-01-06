package com.delivery.SuAl.model.response.basket;

import com.delivery.SuAl.model.response.marketing.EligibleCampaignsResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BasketCalculationResponse {
    private BigDecimal subtotal;
    private BigDecimal totalDepositCharged;
    private BigDecimal totalDepositRefunded;
    private BigDecimal netDeposit;
    private String promoCode;
    private BigDecimal promoDiscount;
    private Boolean promoValid;
    private String promoMessage;
    private BigDecimal campaignDiscount;
    private EligibleCampaignsResponse eligibleCampaigns;
    private BigDecimal amount;
    private BigDecimal totalAmount;
    private Integer totalItems;
    private List<BasketItemResponse> items;
}
