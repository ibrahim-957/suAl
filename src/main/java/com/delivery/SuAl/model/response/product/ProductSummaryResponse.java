package com.delivery.SuAl.model.response.product;

import com.delivery.SuAl.model.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductSummaryResponse {
    private Long id;
    private String name;
    private String companyName;
    private String size;
    private BigDecimal sellPrice;
    private BigDecimal depositAmount;
    private ProductStatus productStatus;
}
