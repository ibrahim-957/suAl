package com.delivery.SuAl.entity;

import com.delivery.SuAl.model.CampaignStatus;
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

    @Column(name = "campaign_id", nullable = false, unique = true, updatable = false)
    private String campaignId;

    @Column(nullable = false)
    private String name;

    private String description;

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
                && !now.isAfter(validFrom)
                && !now.isBefore(validTo);
    }

    public boolean hasReachedTotalLimit(){
        return maxTotalUses != null && currentTotalUses > maxTotalUses;
    }

    public void incrementUses(){
        this.currentTotalUses++;
    }
}