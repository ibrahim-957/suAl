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

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_type", nullable = false)
    private CampaignType campaignType = CampaignType.BUY_X_GET_Y_FREE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buy_product_id", nullable = false)
    private Product buyProduct;

    @Column(name = "buy_quantity", nullable = false)
    private int buyQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "free_product_id", nullable = false)
    private Product freeProduct;

    @Column(name = "free_quantity", nullable = false)
    private int freeQuantity;

    @Column(name = "first_order_only")
    private Boolean firstOrderOnly = false;

    @Column(name = "min_days_since_registration")
    private Integer minDaysSinceRegistration;

    @Column(name = "requires_promo_absence")
    private Boolean requiresPromoAbsence = false;

    @Column(name = "max_uses_per_user")
    private Integer maxUsesPerUser;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    private boolean meetsRegistrationRequirement(LocalDateTime userRegistrationDate){
        if (minDaysSinceRegistration == null || minDaysSinceRegistration == 0){
            return true;
        }

        LocalDateTime requiredDate = userRegistrationDate.plusDays(minDaysSinceRegistration);
        return LocalDateTime.now().isAfter(requiredDate);
    }

    public boolean isFirstOrderOnly(){
        return Boolean.TRUE.equals(firstOrderOnly);
    }

    public boolean allowPromoCode(){
        return !Boolean.TRUE.equals(requiresPromoAbsence);
    }
}