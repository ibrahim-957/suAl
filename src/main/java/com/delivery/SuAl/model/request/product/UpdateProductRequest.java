package com.delivery.SuAl.model.request.product;

import com.delivery.SuAl.model.enums.ProductStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProductRequest {
    @Size(min = 2, max = 255)
    private String name;

    private Long companyId;

    private Long categoryId;

    private String size;

    private BigDecimal buyPrice;

    private BigDecimal sellPrice;

    @DecimalMin(value = "0.0", inclusive = true, message = "Deposit amount cannot be negative")
    @DecimalMax(value = "999.99", message = "Deposit amount is too large")
    @Digits(integer = 3, fraction = 2, message = "Deposit amount must have at most 3 digits before decimal and 2 after")
    private BigDecimal depositAmount;

    private ProductStatus  productStatus;
}
