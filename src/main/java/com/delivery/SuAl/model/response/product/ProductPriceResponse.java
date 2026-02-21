package com.delivery.SuAl.model.response.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductPriceResponse {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal sellPrice;
    private BigDecimal discountPercent;
    private BigDecimal effectivePrice;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private String createdBy;
    private LocalDateTime createdAt;
}
