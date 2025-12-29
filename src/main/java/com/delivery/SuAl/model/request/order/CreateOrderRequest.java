package com.delivery.SuAl.model.request.order;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderRequest {
    private Long userId;

    @NotNull(message = "Address ID is required")
    private Long addressId;

    @NotEmpty(message = "Order must have at least one item")
    @Size(min = 1, message = "Order must have at least one item")
    private List<OrderItemRequest> items;

    private String promoCode;

    private String campaignId;

    private Long campaignProductId;

    private LocalDate deliveryDate;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
