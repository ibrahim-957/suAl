package com.delivery.SuAl.model.request.order;

import com.delivery.SuAl.model.request.cart.CartItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderByUserRequest {
    @NotNull
    private Long addressId;

    @NotNull
    private LocalDate deliveryDate;

    @NotEmpty
    @Valid
    private List<CartItem> items;

    private String promoCode;

    private String note;
}
