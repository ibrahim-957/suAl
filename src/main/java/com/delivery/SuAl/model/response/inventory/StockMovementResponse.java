package com.delivery.SuAl.model.response.inventory;

import com.delivery.SuAl.model.enums.MovementType;
import com.delivery.SuAl.model.enums.ReferenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockMovementResponse {
    private Long id;
    private Long productId;
    private String productName;
    private Long warehouseId;
    private String warehouseName;
    private MovementType movementType;
    private ReferenceType referenceType;
    private Long referenceId;
    private Integer quantity;
    private String notes;
    private LocalDateTime createdAt;
}
