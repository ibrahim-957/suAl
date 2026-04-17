package com.delivery.SuAl.model.request.purchase;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseInvoiceItemRequest {
    @NotNull
    private Long productId;

    @NotNull
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull
    @DecimalMin(value = "0.01", message = "Purchase price must be greater than 0")
    @Digits(integer = 6, fraction = 2)
    private BigDecimal purchasePrice;

    @NotNull
    @DecimalMin(value = "0.01", message = "Sale price must be greater than 0")
    @Digits(integer = 6, fraction = 2)
    private BigDecimal salePrice;

    @DecimalMin(value = "0.00")
    @Digits(integer = 4, fraction = 2)
    private BigDecimal depositUnitAmount;
}
