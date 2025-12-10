package com.delivery.SuAl.model.request.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @NotBlank(message = "Customer name is required")
    @Size(max = 255, message = "Customer name must not exceed 255 characters")
    private String customerName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotNull(message = "Address ID is required")
    private Long addressId;

    @NotEmpty(message = "Order must have at least one item")
    @Size(min = 1, message = "Order must have at least one item")
    private List<OrderItemRequest> items;

    private String promoCode;

    private LocalDate deliveryDate;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
