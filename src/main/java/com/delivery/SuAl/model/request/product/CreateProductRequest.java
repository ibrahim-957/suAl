package com.delivery.SuAl.model.request.product;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateProductRequest {
    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255)
    private String name;

    @NotNull
    private Long companyId;

    @NotNull
    private Long categoryId;

    @NotNull
    private Long warehouseId;

    @Size(max = 50)
    private String size;

    @DecimalMin(value = "0.0", inclusive = true, message = "Deposit amount cannot be negative")
    @DecimalMax(value = "999.99", message = "Deposit amount is too large")
    @Digits(integer = 3, fraction = 2, message = "Deposit amount must have at most 3 digits before decimal and 2 after")
    private BigDecimal depositAmount;

    private BigDecimal buyPrice;

    private BigDecimal sellPrice;

    @NotNull
    @Min(value = 0)
    private Integer initialFullCount;

    @NotNull
    @Min(value = 0)
    private Integer initialEmptyCount;

    @NotNull
    @Min(value = 0)
    private Integer initialDamagedCount;

    @Min(value = 1)
    private Integer minimumStockAlert;
}
