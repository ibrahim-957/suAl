package com.delivery.SuAl.model.response.product;

import com.delivery.SuAl.model.enums.ProductStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Map<String, String> mineralComposition;
    private String companyName;
    private String categoryName;
    private ProductSizeResponse size;
    private BigDecimal depositAmount;
    private boolean hasDeposit;
    private ProductStatus productStatus;
    private boolean returnable;
    private boolean hasDepositAndReturnable;


    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal sellPrice;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal discountPercent;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal effectivePrice;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}