package com.delivery.SuAl.model.response.order;

import com.delivery.SuAl.model.OrderStatus;
import com.delivery.SuAl.model.PaymentMethod;
import com.delivery.SuAl.model.PaymentStatus;
import com.delivery.SuAl.model.response.address.AddressResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private String customerName;
    private String phoneNumber;
    private Long operatorId;
    private String operatorName;
    private Long driverId;
    private String driverName;
    private AddressResponse address;
    private Integer totalItems;
    private Integer emptyBottlesExpected;
    private Integer emptyBottlesCollected;
    private BigDecimal subtotal;
    private BigDecimal promoDiscount;
    private BigDecimal campaignDiscount;
    private BigDecimal totalAmount;
    private BigDecimal totalDepositCharged;
    private BigDecimal totalDepositRefunded;
    private BigDecimal netDeposit;
    private BigDecimal finalAmount;
    private LocalDate deliveryDate;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private LocalDateTime paidAt;
    private LocalDateTime completedAt;
    private String rejectionReason;
    private String promoCode;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderDetailResponse> orderDetails;
    private List<OrderCampaignBonusResponse> campaignBonuses;
}