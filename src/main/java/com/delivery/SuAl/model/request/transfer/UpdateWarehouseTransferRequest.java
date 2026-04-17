package com.delivery.SuAl.model.request.transfer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateWarehouseTransferRequest {
    @Size(max = 1000)
    private String notes;

    @Valid
    private List<WarehouseTransferItemRequest> items;
}
