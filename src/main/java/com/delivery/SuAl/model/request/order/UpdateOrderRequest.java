package com.delivery.SuAl.model.request.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateOrderRequest {
    @Size(max = 255, message = "Customer name must not exceed 255 characters")
    private String customerName;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phoneNumber;

    @Min(value = 0)
    private Integer emptyBottlesExpected;

    private Long addressId;

    private LocalDate deliveryDate;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
