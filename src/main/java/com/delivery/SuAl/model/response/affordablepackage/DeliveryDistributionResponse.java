package com.delivery.SuAl.model.response.affordablepackage;

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
public class DeliveryDistributionResponse {
    private Long id;
    private Integer deliveryNumber;
    private LocalDate deliveryDate;
    private Long addressId;
    private String addressFullAddress;
    private List<DeliveryProductResponse> products;
    private Integer totalQuantity;
}
