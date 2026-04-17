package com.delivery.SuAl.model.request.transfer;

import jakarta.validation.Valid;
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
public class CreateWarehouseTransferRequest {
    @NotNull(message = "Source warehouse is required")
    private Long fromWarehouseId;

    @NotNull(message = "Destination warehouse is required")
    private Long toWarehouseId;

    @Size(max = 1000)
    private String notes;

    @NotEmpty(message = "Transfer must have at least one item")
    @Valid
    private List<WarehouseTransferItemRequest> items;
}
