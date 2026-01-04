package com.delivery.SuAl.model.response.basket;

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
public class BasketResponse {
    private Long id;
    private Long userId;
    private List<BasketItemResponse> items;
    private Integer totalItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
