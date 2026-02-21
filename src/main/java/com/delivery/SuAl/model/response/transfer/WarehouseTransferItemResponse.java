package com.delivery.SuAl.model.response.transfer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseTransferItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSize;
    private String companyName;
    private Integer quantity;
}
