package com.delivery.SuAl.model.request.operation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCarRequest {
    private Long driverId;

    private String brand;

    private String model;

    private String plateNumber;
}
