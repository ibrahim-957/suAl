package com.delivery.SuAl.helper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FreeProductSummary {
    private Long productId;
    private String productName;
    private Integer totalQuantity;
    private BigDecimal pricePerUnit;
    private BigDecimal totalValue;
    private Boolean hasDeposit;
    private BigDecimal depositPerUnit;
    private BigDecimal totalDeposit;
}
