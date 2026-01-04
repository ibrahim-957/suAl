package com.delivery.SuAl.model.request.basket;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RemoveFromBasketRequest {
    @NotNull
    private Long basketItemId;
}
