package com.delivery.SuAl.model.response.transfer;

import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.model.enums.TransferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WarehouseTransferResponse {
    private Long id;
    private String transferNumber;
    private Long fromWarehouseId;
    private String fromWarehouseName;
    private Long toWarehouseId;
    private String toWarehouseName;
    private TransferStatus status;
    private User createdBy;
    private String notes;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<WarehouseTransferItemResponse> items;
}
