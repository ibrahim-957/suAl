package com.delivery.SuAl.model.response.purchase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PurchaseInvoiceItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSize;
    private String companyName;
    private Integer quantity;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private BigDecimal depositUnitAmount;
    private BigDecimal lineTotal;
}
