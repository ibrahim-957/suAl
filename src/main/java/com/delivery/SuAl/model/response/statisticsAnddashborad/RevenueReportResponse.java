package com.delivery.SuAl.model.response.statisticsAnddashborad;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RevenueReportResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long totalOrders;
    private Long completedOrders;
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal totalProfit;
    private BigDecimal profitMargin;
    private BigDecimal averageOrderValue;
    private List<DailyRevenueResponse> dailyBreakdown;
}
