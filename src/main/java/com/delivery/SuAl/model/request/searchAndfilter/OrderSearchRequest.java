package com.delivery.SuAl.model.request.searchAndfilter;

import com.delivery.SuAl.model.enums.OrderStatus;
import com.delivery.SuAl.model.enums.PaymentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderSearchRequest {
    private String orderNumber;
    private String customerName;
    private String phoneNumber;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private Long driverId;
    private Long operatorId;
    private LocalDate deliveryDateFrom;
    private LocalDate deliveryDateTo;
    private LocalDate createdDateFrom;
    private LocalDate createdDateTo;

    @Min(value = 0, message = "Page number cannot be negative")
    private Integer page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    private Integer pageSize = 20;

    private String sortBy = "createdAt";
    private String sortDirection = "DESC";
}
