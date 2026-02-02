package com.delivery.SuAl.model.response.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerCollectionResponse {
    private Long collectionId;
    private Long warehouseId;
    private String warehouseName;
    private Long productId;
    private String productName;
    private Integer emptyContainersCollected;
    private Integer damagedContainersCollected;
    private Integer totalCollected;
    private String collectedBy;
    private LocalDateTime collectionDateTime;
    private String notes;
}
