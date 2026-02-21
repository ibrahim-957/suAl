package com.delivery.SuAl.model.response.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockBatchResponse {
    private Long id;
    private Long productId;
    private String productName;
    private Long warehouseId;
    private String warehouseName;
    private Long purchaseInvoiceItemId;
    private String invoiceNumber;
    private Integer initialQuantity;
    private Integer remainingQuantity;
    private Integer consumedQuantity;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private LocalDateTime createdAt;
}
