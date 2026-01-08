package com.delivery.SuAl.model.request.searchAndfilter;

import com.delivery.SuAl.model.enums.DriverStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DriverSearchRequest {
    private String name;

    private String surname;

    private String phoneNumber;

    private DriverStatus driverStatus;

    private Boolean availableOnly;


    @Min(value = 0)
    private Integer page = 0;

    @Min(value = 1)
    @Max(value = 100)
    private Integer pageSize = 20;

    private String sortBy = "name";
    private String sortDirection = "ASC";
}
