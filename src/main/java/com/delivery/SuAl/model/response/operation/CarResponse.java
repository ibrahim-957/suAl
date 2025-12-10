package com.delivery.SuAl.model.response.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CarResponse {
    private Long id;
    private Long driverId;
    private String driverName;
    private String brand;
    private String model;
    private String plateNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
