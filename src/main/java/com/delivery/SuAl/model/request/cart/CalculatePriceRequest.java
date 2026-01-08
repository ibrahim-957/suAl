package com.delivery.SuAl.model.request.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalculatePriceRequest {
    @NotNull
    private Long userId;

    @NotEmpty
    @Valid
    private List<CartItem> items;

    private String promoCode;
}
