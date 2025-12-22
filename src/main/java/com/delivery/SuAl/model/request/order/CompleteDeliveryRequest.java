package com.delivery.SuAl.model.request.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompleteDeliveryRequest {
    private String notes;

    private List<BottleCollectionItem> bottlesCollected;
}
