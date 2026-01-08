package com.delivery.SuAl.model.request.marketing;

import com.delivery.SuAl.entity.Order;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApplyCampaignRequest {
    @NotBlank
    private String campaignCode;
    @NotNull
    private Long userId;


    private Order order;
}