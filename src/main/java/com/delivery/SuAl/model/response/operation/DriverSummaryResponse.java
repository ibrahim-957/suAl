package com.delivery.SuAl.model.response.operation;

import com.delivery.SuAl.model.DriverStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DriverSummaryResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private DriverStatus driverStatus;
    private Boolean available;
}
