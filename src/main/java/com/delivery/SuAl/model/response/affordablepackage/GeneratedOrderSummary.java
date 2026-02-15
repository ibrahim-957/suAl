package com.delivery.SuAl.model.response.affordablepackage;

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
public class GeneratedOrderSummary {
    private Long orderId;
    private String orderNumber;
    private Integer deliveryNumber;
    private String orderStatus;
    private LocalDate deliveryDate;
    private BigDecimal totalAmount;
}
