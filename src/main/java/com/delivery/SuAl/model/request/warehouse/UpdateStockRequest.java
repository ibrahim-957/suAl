package com.delivery.SuAl.model.request.warehouse;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateStockRequest {
    @NotNull
    private Long productId;

    @Min(value = 0)
    private Integer fullCount;

    @Min(value = 0)
    private Integer emptyCount;

    @Min(value = 0)
    private Integer damagedCount;
}
