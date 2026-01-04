package com.delivery.SuAl.model.request.basket;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddToBasketByOperatorRequest {
    @NotNull
    Long userId;

    @NotNull
    Long productId;

    @NotNull
    @Min(1)
    Integer quantity;
}
