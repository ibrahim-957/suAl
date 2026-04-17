package com.delivery.SuAl.model.request.transfer;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WarehouseTransferItemRequest {
    @NotNull
    private Long productId;

    @NotNull
    @Min(value = 1, message = "Transfer quantity must be at least 1")
    private Integer quantity;
}
