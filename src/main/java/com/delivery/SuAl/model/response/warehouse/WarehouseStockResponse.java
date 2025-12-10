package com.delivery.SuAl.model.response.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WarehouseStockResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String companyName;
    private String size;
    private Integer fullCount;
    private Integer emptyCount;
    private Integer damagedCount;
    private Integer totalCount;
    private Integer minimumStockAlert;
    private Boolean lowStock;
    private Boolean outOfStock;
    private LocalDateTime lastRestocked;
    private LocalDateTime updatedAt;
}
