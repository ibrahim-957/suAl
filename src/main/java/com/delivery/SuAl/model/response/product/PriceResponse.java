package com.delivery.SuAl.model.response.product;

import com.delivery.SuAl.model.CategoryType;
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
public class PriceResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String companyName;
    private CategoryType categoryType;
    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private BigDecimal profitMargin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
