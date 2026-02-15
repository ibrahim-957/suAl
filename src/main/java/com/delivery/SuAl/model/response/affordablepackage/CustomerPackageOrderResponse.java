package com.delivery.SuAl.model.response.affordablepackage;

import com.delivery.SuAl.model.enums.PackageOrderStatus;
import com.delivery.SuAl.model.enums.PaymentMethod;
import com.delivery.SuAl.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerPackageOrderResponse {
    private Long id;
    private String orderNumber;
    private Long customerId;
    private String customerName;
    private Long packageId;
    private String packageName;
    private Integer frequency;
    private BigDecimal packageProductPrice;
    private Integer totalContainersInPackage;
    private Integer oldContainersToCollect;
    private BigDecimal totalDepositCharged;
    private BigDecimal expectedDepositRefunded;
    private BigDecimal actualDepositRefunded;
    private BigDecimal netDeposit;
    private BigDecimal totalPrice;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private BigDecimal amountCollectedAtDelivery1;
    private PackageOrderStatus orderStatus;
    private String orderMonth;
    private Boolean autoRenew;
    private List<DeliveryDistributionResponse> deliveryDistributions;
    private List<GeneratedOrderSummary> generatedOrders;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
