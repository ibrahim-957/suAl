package com.delivery.SuAl.model.response.purchase;

import com.delivery.SuAl.model.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PurchaseInvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private Long CompanyId;
    private String CompanyName;
    private Long warehouseId;
    private String warehouseName;
    private InvoiceStatus status;
    private BigDecimal totalAmount;
    private String notes;
    private LocalDateTime approvedAt;
    private String createdBy;
    private String approvedBy;
    private LocalDate invoiceDate;
    private BigDecimal totalDepositAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PurchaseInvoiceItemResponse> items;
}
