package com.delivery.SuAl.model.response.affordablepackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AffordablePackageResponse {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal totalPrice;
    private Integer maxFrequency;
    private Boolean isActive;
    private Long companyId;
    private String companyName;
    private Integer totalContainers;
    private List<PackageProductResponse> products;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String frequencyDescription;
}
