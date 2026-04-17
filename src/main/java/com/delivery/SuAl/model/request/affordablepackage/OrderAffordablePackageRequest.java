package com.delivery.SuAl.model.request.affordablepackage;

import com.delivery.SuAl.model.enums.PaymentMethod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderAffordablePackageRequest {
    @NotNull
    private Long packageId;

    @NotNull
    @Min(1)
    @Max(4)
    private Integer frequency;

    @NotEmpty
    private List<DeliveryDistributionRequest> distributions;

    @NotNull
    private PaymentMethod paymentMethod;

    private Boolean autoRenew = false;
}
