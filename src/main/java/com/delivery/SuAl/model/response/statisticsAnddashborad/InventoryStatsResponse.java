package com.delivery.SuAl.model.response.statisticsAnddashborad;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryStatsResponse {
    private Integer totalProducts;
    private Integer totalFullBottles;
    private Integer totalEmptyBottles;
    private Integer totalDamagedBottles;
    private Integer lowStockProducts;
    private Integer outOfStockProducts;
    private BigDecimal totalInventoryValue;
    private List<TopProductResponse> topProducts;
}
