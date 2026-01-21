package com.delivery.SuAl.model.response.order;

import com.delivery.SuAl.model.enums.PaymentMethod;
import com.delivery.SuAl.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverCollectionInfoResponse {
    private Long orderId;
    private String orderNumber;
    private String userName;
    private String userPhone;
    private String deliveryAddress;
    private List<ProductDeliverItem> productsToDeliver;
    private Integer totalBottlesExpected;
    private List<BottleCollectionExpectation> expectedCollections;
    private BigDecimal estimatedTotalAmount;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private Boolean hasInsufficientContainers;
    private String collectionWarning;
}
