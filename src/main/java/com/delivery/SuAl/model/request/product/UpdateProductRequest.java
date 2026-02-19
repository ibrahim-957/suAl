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
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProductRequest {
    @Size(min = 2, max = 255)
    private String name;

    private Long companyId;
    private Long categoryId;
    private Long sizeId;

    private BigDecimal sellPrice;

    @Size(max = 2000)
    private String description;

    private Map<String, String> mineralComposition;

    @DecimalMin("0.0") @DecimalMax("999.99") @Digits(integer = 3, fraction = 2)
    private BigDecimal depositAmount;

    private ProductStatus productStatus;
}