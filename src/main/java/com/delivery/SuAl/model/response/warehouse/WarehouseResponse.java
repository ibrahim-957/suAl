package com.delivery.SuAl.model.response.warehouse;

import com.delivery.SuAl.model.WarehouseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WarehouseResponse {
    private Long id;
    private String name;
    private WarehouseStatus warehouseStatus;
    private Integer totalProducts;
    private Integer totalStockCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
