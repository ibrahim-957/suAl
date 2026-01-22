package com.delivery.SuAl.helper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CampaignApplicationResult {
    private BigDecimal totalDiscount = BigDecimal.ZERO;
    private int appliedCampaignsCount = 0;
    private List<String> freeProducts = new ArrayList<>();
    private List<String> discountedProducts = new ArrayList<>();
    private List<String> bonusDiscounts = new ArrayList<>();

    public void addDiscount(BigDecimal amount) {
        if (amount != null) {
            this.totalDiscount = this.totalDiscount.add(amount);
        }
    }

    public void incrementAppliedCampaigns() {
        this.appliedCampaignsCount++;
    }

    public void addFreeProduct(String productName, int quantity) {
        this.freeProducts.add(String.format("%s x%d", productName, quantity));
    }

    public void addDiscountedProduct(String productName, int quantity) {
        this.discountedProducts.add(String.format("%s x%d (discounted)", productName, quantity));
    }

    public void addBonusDiscount(String bonusName, BigDecimal amount) {
        this.bonusDiscounts.add(String.format("%s: %s AZN", bonusName, amount));
    }
}
