package com.delivery.SuAl.model.response.purchase;

import com.delivery.SuAl.model.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PurchaseInvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private Long supplierId;
    private String supplierName;
    private Long warehouseId;
    private String warehouseName;
    private InvoiceStatus status;
    private BigDecimal totalAmount;
    private String notes;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PurchaseInvoiceItemResponse> items;
}
