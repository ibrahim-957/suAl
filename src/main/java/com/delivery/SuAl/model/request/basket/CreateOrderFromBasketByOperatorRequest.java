package com.delivery.SuAl.model.request.basket;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderFromBasketByOperatorRequest {
    @NotNull
    private Long userId;

    @NotNull
    private Long addressId;

    LocalDate deliveryDate;

    private String promoCode;

    private String campaignId;

    private Long campaignProductId;

    private String notes;
}
