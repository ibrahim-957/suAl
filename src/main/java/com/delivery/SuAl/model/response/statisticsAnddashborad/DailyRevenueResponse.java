package com.delivery.SuAl.model.response.statisticsAnddashborad;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DailyRevenueResponse {
    private LocalDate date;
    private Long orderCount;
    private BigDecimal revenue;
    private BigDecimal profit;
}
