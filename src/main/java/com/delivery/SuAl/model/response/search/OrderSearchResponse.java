package com.delivery.SuAl.model.response.search;

import com.delivery.SuAl.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderSearchResponse {
    private Long id;
    private String orderNumber;
    private String customerName;
    private String phoneNumber;
    private BigDecimal finalAmount;
    private OrderStatus orderStatus;
    private LocalDate deliveryDate;
    private LocalDateTime createdAt;
}
