package com.delivery.SuAl.model.request.order;

import com.delivery.SuAl.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateOrderStatusRequest {
    @NotNull(message = "Order status is required")
    private OrderStatus orderStatus;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
