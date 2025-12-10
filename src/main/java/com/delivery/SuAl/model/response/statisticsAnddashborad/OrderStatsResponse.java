package com.delivery.SuAl.model.response.statisticsAnddashborad;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderStatsResponse {
    private Long totalOrders;
    private Long pendingOrders;
    private Long completedOrders;
    private Long cancelledOrders;
    private BigDecimal totalSpent;
    private BigDecimal averageOrderValue;
    private Integer totalBottlesOrdered;
    private Integer totalBottlesReturned;
    private LocalDateTime firstOrderDate;
    private LocalDateTime lastOrderDate;
}
