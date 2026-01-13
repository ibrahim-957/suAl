package com.delivery.SuAl.model.response.product;

import com.delivery.SuAl.model.enums.CategoryType;
import com.delivery.SuAl.model.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Map<String, String> mineralComposition;
    private String companyName;
    private CategoryType categoryType;
    private String size;
    private BigDecimal depositAmount;
    private ProductStatus productStatus;
    private BigDecimal sellPrice;
    private BigDecimal buyPrice;
    private Long orderCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}