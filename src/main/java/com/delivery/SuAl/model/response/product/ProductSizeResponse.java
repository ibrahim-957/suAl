package com.delivery.SuAl.model.response.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductSizeResponse {
    private Long id;
    private String label;
    private Boolean isActive;
}
