package com.delivery.SuAl.model.request.product;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePriceRequest {
    @NotNull(message = "Buy price is required")
    @DecimalMin(value = "0.01", message = "Buy price must be greater than 0")
    @DecimalMax(value = "999.99", message = "Buy price is too large")
    @Digits(integer = 3, fraction = 2, message = "Buy price must have at most 3 digits before decimal and 2 after")
    private BigDecimal buyPrice;

    @NotNull(message = "Sell price is required")
    @DecimalMin(value = "0.01", message = "Sell price must be greater than 0")
    @DecimalMax(value = "999.99", message = "Sell price is too large")
    @Digits(integer = 3, fraction = 2, message = "Sell price must have at most 3 digits before decimal and 2 after")
    private BigDecimal sellPrice;
}
