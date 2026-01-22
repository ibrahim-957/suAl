package com.delivery.SuAl.entity;

import com.delivery.SuAl.model.enums.CampaignType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_campaign_bonuses")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderCampaignBonus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference("order-campaign-bonus")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "bonus_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal bonusValue;

    private String bonusType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (bonusType == null && campaign != null) {
            bonusType = determineBonusType(campaign.getCampaignType());
        }
    }

    private String determineBonusType(CampaignType campaignType) {
        return switch (campaignType) {
            case BUY_X_GET_Y_FREE -> "FREE_PRODUCT";
            case BUY_X_PAY_FOR_Y -> "DISCOUNTED_PRODUCT";
            case FIRST_ORDER_BONUS -> "FIRST_ORDER_BONUS";
            case LOYALTY_BONUS -> "LOYALTY_BONUS";
        };
    }

    public boolean isFreeProductBonus() {
        return "FREE_PRODUCT".equals(bonusType);
    }

    public boolean isDiscountedProductBonus() {
        return "DISCOUNTED_PRODUCT".equals(bonusType);
    }

    public boolean isFirstOrderBonus() {
        return "FIRST_ORDER_BONUS".equals(bonusType);
    }

    public boolean isLoyaltyBonus() {
        return "LOYALTY_BONUS".equals(bonusType);
    }

    public boolean hasPhysicalProduct() {
        return product != null && quantity != null && quantity > 0;
    }

    public String getDescription() {
        if (isFreeProductBonus() && product != null) {
            return String.format("Free: %s x%d", product.getName(), quantity);
        } else if (isDiscountedProductBonus() && product != null) {
            return String.format("Discount on: %s x%d", product.getName(), quantity);
        } else if (isFirstOrderBonus()) {
            return "First Order Bonus";
        } else if (isLoyaltyBonus()) {
            return "Loyalty Bonus";
        }
        return "Campaign Bonus";
    }
}