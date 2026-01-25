package com.delivery.SuAl.entity;

import com.delivery.SuAl.model.enums.CampaignStatus;
import com.delivery.SuAl.model.enums.CampaignType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_code", unique = true, nullable = false, updatable = false)
    private String campaignCode;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_type", nullable = false)
    private CampaignType campaignType = CampaignType.BUY_X_GET_Y_FREE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buy_product_id")
    private Product buyProduct;

    @Column(name = "buy_quantity")
    private Integer buyQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "free_product_id")
    private Product freeProduct;

    @Column(name = "free_quantity")
    private Integer freeQuantity;

    @Column(name = "bonus_amount", precision = 10, scale = 2)
    private BigDecimal bonusAmount;

    @Column(name = "bonus_percentage", precision = 15, scale = 2)
    private BigDecimal bonusPercentage;

    @Column(name = "first_order_only")
    private Boolean firstOrderOnly = false;

    @Column(name = "min_days_since_registration")
    private Integer minDaysSinceRegistration;

    @Column(name = "requires_promo_absence")
    private Boolean requiresPromoAbsence = false;

    @Column(name = "max_uses_per_customer")
    private Integer maxUsesPerCustomer;

    @Column(name = "max_total_uses")
    private Integer maxTotalUses;

    @Column(name = "current_total_uses", nullable = false)
    private int currentTotalUses = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_status", nullable = false)
    private CampaignStatus campaignStatus = CampaignStatus.ACTIVE;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        validateCampaignFields();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        validateCampaignFields();
    }

    public boolean isActive(){
        LocalDate now = LocalDate.now();
        return campaignStatus == CampaignStatus.ACTIVE
                && !now.isAfter(validTo)
                && !now.isBefore(validFrom);
    }

    public boolean hasReachedTotalLimit(){
        return maxTotalUses != null && currentTotalUses >= maxTotalUses;
    }

    public void incrementUses(){
        this.currentTotalUses++;
    }

    public boolean isFirstOrderOnly(){
        return Boolean.TRUE.equals(firstOrderOnly);
    }

    public boolean allowPromoCode(){
        return !Boolean.TRUE.equals(requiresPromoAbsence);
    }

    public boolean isProductBasedCampaign(){
        return campaignType == CampaignType.BUY_X_GET_Y_FREE ||
                campaignType == CampaignType.BUY_X_PAY_FOR_Y;
    }

    public boolean isBonusBasedCampaign(){
        return campaignType == CampaignType.FIRST_ORDER_BONUS ||
                campaignType == CampaignType.LOYALTY_BONUS;
    }

    private void validateCampaignFields() {
        switch (campaignType) {
            case BUY_X_GET_Y_FREE:
            case BUY_X_PAY_FOR_Y:
                if (buyProduct == null || freeProduct == null) {
                    throw new IllegalStateException(
                            campaignType + " requires both buyProduct and freeProduct");
                }
                if (buyQuantity == null || buyQuantity <= 0) {
                    throw new IllegalStateException(
                            campaignType + " requires valid buyQuantity");
                }
                if (freeQuantity == null || freeQuantity <= 0) {
                    throw new IllegalStateException(
                            campaignType + " requires valid freeQuantity");
                }
                break;

            case FIRST_ORDER_BONUS:
                if (bonusAmount == null || bonusAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalStateException(
                            "FIRST_ORDER_BONUS requires valid bonusAmount");
                }
                this.firstOrderOnly = true;
                break;

            case LOYALTY_BONUS:
                if (minDaysSinceRegistration == null || minDaysSinceRegistration <= 0) {
                    throw new IllegalStateException(
                            "LOYALTY_BONUS requires minDaysSinceRegistration");
                }
                if ((bonusAmount == null || bonusAmount.compareTo(BigDecimal.ZERO) <= 0) &&
                        (bonusPercentage == null || bonusPercentage.compareTo(BigDecimal.ZERO) <= 0)) {
                    throw new IllegalStateException(
                            "LOYALTY_BONUS requires either bonusAmount or bonusPercentage");
                }
                break;
        }
    }
}