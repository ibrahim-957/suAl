package com.delivery.SuAl.model.request.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePurchaseInvoiceRequest {

    @NotNull(message = "Supplier ID is required")
    private Long companyId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotBlank(message = "Invoice number is required")
    @Size(max = 100)
    private String invoiceNumber;

    @Size(max = 1000)
    private String notes;

    @NotEmpty(message = "Invoice must have at least one item")
    @Valid
    private List<PurchaseInvoiceItemRequest> items;
}
