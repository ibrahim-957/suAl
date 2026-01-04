package com.delivery.SuAl.model.request.basket;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateOrderFromBasketRequest {
    @NotNull
    private Long addressId;

    private LocalDate deliveryDate;

    private String promoCode;

    private String campaignId;

    private Long campaignProductId;

    private String notes;
}
