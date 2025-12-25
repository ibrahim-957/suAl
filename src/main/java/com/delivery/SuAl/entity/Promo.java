package com.delivery.SuAl.entity;

import com.delivery.SuAl.model.DiscountType;
import com.delivery.SuAl.model.PromoStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "promos")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Promo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "promo_code", nullable = false, unique = true)
    private String promoCode;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "max_uses_per_user")
    private Integer maxUsesPerUser;

    @Column(name = "max_total_uses")
    private Integer maxTotalUses;

    @Column(name = "current_total_uses", nullable = false)
    private int currentTotalUses = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "promo_status", nullable = false)
    private PromoStatus promoStatus = PromoStatus.ACTIVE;

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
        return promoStatus == PromoStatus.ACTIVE
                && !now.isAfter(validFrom)
                && !now.isBefore(validTo);
    }

    public boolean hasReachedTotalLimit(){
        return maxTotalUses != null && currentTotalUses > maxTotalUses;
    }

    public void incrementUses(){
        this.currentTotalUses++;
    }

    public BigDecimal calculateDiscount(BigDecimal orderAmount){
        if (orderAmount.compareTo(minOrderAmount) < 0){
            throw  new IllegalArgumentException("Order amount must be greater than minimum amount");
        }

        BigDecimal discount;
        if (discountType == DiscountType.PERCENTAGE){
            discount = orderAmount.multiply(discountValue).divide(BigDecimal.valueOf(100));

            if (maxDiscount != null && discount.compareTo(maxDiscount) > 0){
                discount = maxDiscount;
            }
        } else {
            discount = discountValue;
        }
        return discount;
    }
}