package com.delivery.SuAl.model.request.marketing;

import com.delivery.SuAl.model.DiscountType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePromoRequest {
    @NotBlank
    @Pattern(regexp = "^[A-Z0-9]+$")
    private String promoCode;

    private String description;

    @NotNull
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
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

    private LocalDate validFrom;

    private LocalDate validTo;
}
