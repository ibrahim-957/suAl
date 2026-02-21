package com.delivery.SuAl.model.request.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePurchaseInvoiceRequest {
    @Size(max = 100)
    private String invoiceNumber;

    private Long companyId;

    @Size(max = 1000)
    private String notes;

    @Valid
    private List<PurchaseInvoiceItemRequest> items;
}
