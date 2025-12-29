package com.delivery.SuAl.model.request.marketing;

import com.delivery.SuAl.model.DiscountType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePromoRequest {

    @NotBlank(message = "Promo code is required")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Promo code must contain only uppercase letters and numbers")
    @Size(min = 4, max = 20, message = "Promo code must be between 4 and 20 characters")
    private String promoCode;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Invalid discount value format")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.0", message = "Minimum order amount cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid minimum order amount format")
    private BigDecimal minOrderAmount;

    @DecimalMin(value = "0.01", message = "Maximum discount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Invalid maximum discount format")
    private BigDecimal maxDiscount;

    @Min(value = 1, message = "Max uses per user must be at least 1")
    private Integer maxUsesPerUser;

    @Min(value = 1, message = "Max total uses must be at least 1")
    private Integer maxTotalUses;

    @NotNull(message = "Valid from date is required")
    private LocalDate validFrom;

    @NotNull(message = "Valid to date is required")
    private LocalDate validTo;

    @AssertTrue(message = "For percentage discount, value must be between 0 and 100")
    private boolean isValidPercentage() {
        if (discountType == DiscountType.PERCENTAGE && discountValue != null) {
            return discountValue.compareTo(BigDecimal.ZERO) > 0
                    && discountValue.compareTo(BigDecimal.valueOf(100)) <= 0;
        }
        return true;
    }
}
