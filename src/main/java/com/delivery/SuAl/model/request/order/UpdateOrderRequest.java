package com.delivery.SuAl.model.request.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateOrderRequest {

    private Long addressId;

    @Min(0)
    private Integer emptyBottlesExpected;

    @Size(max = 1000)
    private String notes;

    private LocalDate deliveryDate;

    private List<UpdateOrderItemRequest> items;
}

