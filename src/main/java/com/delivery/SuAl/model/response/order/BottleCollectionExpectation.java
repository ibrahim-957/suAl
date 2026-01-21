package com.delivery.SuAl.model.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BottleCollectionExpectation {
    private Long productId;
    private String productName;
    private Integer expectedToCollect;
    private Integer userHasAvailable;
    private Integer shortfall;
    private BigDecimal extraDepositPerBottle;
    private BigDecimal totalExtraDeposit;
    private String warningMessage;
}
