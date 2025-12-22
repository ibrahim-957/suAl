package com.delivery.SuAl.model.request.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateOrderItemRequest {
    private Long orderDetailId;

    @Min(1)
    private Integer quantity;

    @DecimalMin("0.0")
    private BigDecimal sellPrice;
}