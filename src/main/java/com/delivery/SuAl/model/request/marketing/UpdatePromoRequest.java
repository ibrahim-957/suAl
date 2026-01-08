package com.delivery.SuAl.model.request.marketing;

import com.delivery.SuAl.model.enums.DiscountType;
import com.delivery.SuAl.model.enums.PromoStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePromoRequest {
    private String description;

    private DiscountType  discountType;

    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    @DecimalMax(value = "99.99", message = "Discount value is too large")
    @Digits(integer = 2, fraction = 2, message = "Discount value must have at most 2 digits before decimal and 2 after")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.0")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal minOrderAmount;

    @DecimalMin(value = "0.1")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal maxDiscount;

    private Integer maxUsesPerUser;

    private Integer maxTotalUses;

    private PromoStatus  promoStatus;

    private LocalDate validFrom;

    private LocalDate validTo;
}