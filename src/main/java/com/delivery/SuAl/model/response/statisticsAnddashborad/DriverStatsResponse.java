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
public class DriverStatsResponse {
    private Long driverId;
    private String driverName;
    private Long totalDeliveries;
    private Long deliveriesToday;
    private Long deliveriesThisWeek;
    private Long deliveriesThisMonth;
    private BigDecimal totalRevenue;
    private BigDecimal averageDeliveryValue;
    private Double onTimeDeliveryRate;
    private LocalDateTime lastDeliveryDate;
}
