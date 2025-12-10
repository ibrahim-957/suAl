package com.delivery.SuAl.model.request.warehouse;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateWarehouseRequest {
    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Company ID is required")
    private Long companyId;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @Min(value = 0, message = "Full count cannot be negative")
    private Integer fullCount = 0;

    @Min(value = 0, message = "Empty count cannot be negative")
    private Integer emptyCount = 0;

    @Min(value = 0, message = "Damaged count cannot be negative")
    private Integer damagedCount = 0;

    @Min(value = 0, message = "Minimum stock alert cannot be negative")
    private Integer minimumStockAlert = 10;
}
