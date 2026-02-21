package com.delivery.SuAl.model.request.product;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateProductPriceRequest {
    @NotNull
    private Long productId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Sell price must be greater than 0")
    @DecimalMax(value = "9999.99")
    @Digits(integer = 4, fraction = 2)
    private BigDecimal sellPrice;

    @DecimalMin(value = "0.00")
    @DecimalMax(value = "100.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal discountPercent;

    private LocalDateTime validFrom;
}
