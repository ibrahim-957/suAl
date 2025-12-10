package com.delivery.SuAl.model.response.order;

import com.delivery.SuAl.model.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderDetailResponse {
    private Long id;
    private String productName;
    private String companyName;
    private CategoryType categoryType;
    private String size;
    private Integer quantity;
    private BigDecimal pricePerUnit;
    private BigDecimal subtotal;
    private Integer containersReturned;
    private BigDecimal depositPerUnit;
    private BigDecimal depositCharged;
    private BigDecimal depositRefunded;
    private BigDecimal lineTotal;
}
