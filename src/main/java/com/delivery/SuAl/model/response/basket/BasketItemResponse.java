package com.delivery.SuAl.model.response.basket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BasketItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSize;
    private String companyName;
    private Integer quantity;
    private BigDecimal pricePerUnit;
    private BigDecimal subtotal;
    private BigDecimal depositPerUnit;
    private Integer availableContainers;
    private Integer containersToReturn;
}
