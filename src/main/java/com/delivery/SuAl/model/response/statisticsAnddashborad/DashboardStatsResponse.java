package com.delivery.SuAl.model.response.statisticsAnddashborad;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DashboardStatsResponse {
    private Long totalOrdersToday;
    private Long pendingOrders;
    private Long assignedOrders;
    private Long deliveredOrdersToday;
    private BigDecimal todayRevenue;
    private BigDecimal monthRevenue;
    private Long lowStockProducts;
    private Long outOfStockProducts;
    private Long availableDrivers;
    private Long activePromos;
    private Long activeCampaigns;
}
