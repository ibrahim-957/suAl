package com.delivery.SuAl.model.response.affordablepackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryProductResponse {
    private Long productId;
    private String productName;
    private Integer quantity;
}
