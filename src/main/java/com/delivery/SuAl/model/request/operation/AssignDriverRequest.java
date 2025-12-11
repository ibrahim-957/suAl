package com.delivery.SuAl.model.request.operation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignDriverRequest {
    @NotNull
    private Long driverId;
}
