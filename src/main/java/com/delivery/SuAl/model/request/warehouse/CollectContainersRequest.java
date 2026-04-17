package com.delivery.SuAl.model.request.warehouse;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectContainersRequest {
    @NotNull
    private Long warehouseId;

    @NotNull
    private Long productId;

    @Min(0)
    private Integer emptyContainers = 0;

    @Min(0)
    private Integer damagedContainers = 0;

    private String notes;
}
