package com.delivery.SuAl.model.request.operation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompleteDeliveryRequest {
    @NotNull
    @Min(value = 0)
    private Integer emptyBottlesCollected;

    private String notes;

    private LocalDateTime deliveredAt;
}
