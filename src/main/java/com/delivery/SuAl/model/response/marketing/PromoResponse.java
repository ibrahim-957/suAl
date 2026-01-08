package com.delivery.SuAl.model.response.marketing;

import com.delivery.SuAl.model.enums.DiscountType;
import com.delivery.SuAl.model.enums.PromoStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PromoResponse {
    private Long id;
    private String promoCode;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscount;
    private Integer maxUsesPerUser;
    private Integer maxTotalUses;
    private Integer currentTotalUses;
    private Integer usageRemaining;
    private PromoStatus promoStatus;
    private LocalDate validFrom;
    private LocalDate validTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
