package com.delivery.SuAl.model.response.operation;

import com.delivery.SuAl.model.enums.DriverStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DriverResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private DriverStatus driverStatus;
    private Boolean available;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
