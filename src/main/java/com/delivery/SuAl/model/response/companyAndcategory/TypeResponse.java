package com.delivery.SuAl.model.response.companyAndcategory;

import com.delivery.SuAl.model.ContainerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TypeResponse {
    private Long id;
    private ContainerType containerType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
