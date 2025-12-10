package com.delivery.SuAl.model.response.search;

import com.delivery.SuAl.model.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductSearchResponse {
    private Long id;
    private String name;
    private String companyName;
    private String size;
    private BigDecimal sellPrice;
    private ProductStatus productStatus;
    private Integer stockCount;

}
