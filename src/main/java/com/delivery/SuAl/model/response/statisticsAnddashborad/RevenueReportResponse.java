package com.delivery.SuAl.model.response.statisticsAnddashborad;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    private BigDecimal averageOrderValue;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal totalCost;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal totalProfit;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal profitMargin;

    private List<DailyRevenueResponse> dailyBreakdown;
}
