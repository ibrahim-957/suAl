package com.delivery.SuAl.model.request.basket;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateBasketItemRequest {
    @NotNull
    private Long basketItemId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
