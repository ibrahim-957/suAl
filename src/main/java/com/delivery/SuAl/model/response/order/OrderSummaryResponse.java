package com.delivery.SuAl.model.response.order;

import com.delivery.SuAl.model.OrderStatus;
import com.delivery.SuAl.model.PaymentStatus;
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
public class OrderSummaryResponse {
    private Long id;
    private String orderNumber;
    private String customerName;
    private String phoneNumber;
    private Integer totalItems;
    private BigDecimal finalAmount;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private LocalDate deliveryDate;
    private String driverName;
    private LocalDateTime createdAt;
}
