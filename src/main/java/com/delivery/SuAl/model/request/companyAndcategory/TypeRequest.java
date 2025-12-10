package com.delivery.SuAl.model.request.companyAndcategory;

import com.delivery.SuAl.model.ContainerType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TypeRequest {
    @NotNull
    private ContainerType containerType;
}
