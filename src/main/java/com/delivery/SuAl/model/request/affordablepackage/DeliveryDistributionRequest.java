package com.delivery.SuAl.model.request.affordablepackage;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeliveryDistributionRequest {
    @NotNull
    @Min(1)
    @Max(4)
    private Integer deliveryNumber;

    @NotNull
    private LocalDate deliveryDate;

    @NotNull
    private Long addressId;

    @NotEmpty
    private List<DeliveryProductRequest> products;
}
