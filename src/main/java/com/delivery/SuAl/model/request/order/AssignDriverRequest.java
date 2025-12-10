package com.delivery.SuAl.model.request.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignDriverRequest {
    @NotNull(message = "Driver ID is required")
    private Long driverId;

    @NotNull(message = "Operator ID is required")
    private Long operatorId;

    private LocalDate scheduledDeliveryDate;

    @Size(max = 500)
    private String notes;
}
