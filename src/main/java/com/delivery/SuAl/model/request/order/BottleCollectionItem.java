package com.delivery.SuAl.model.request.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BottleCollectionItem {
    private Long productId;
    private Integer quantity;
}
