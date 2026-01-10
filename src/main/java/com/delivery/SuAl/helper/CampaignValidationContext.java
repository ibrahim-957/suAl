package com.delivery.SuAl.helper;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CampaignValidationContext {
    private boolean isValid = true;
    private StringBuilder messageBuilder = new StringBuilder();

    private boolean meetsDateRequirement;
    private boolean meetsQuantityRequirement;
    private boolean meetsFirstOrderRequirement;
    private boolean meetsUsageLimitRequirement;
    private boolean meetsPromoAbsenceRequirement;

    private Integer basketQuantity;
    private Integer userUsageCount;
    private Integer usageRemaining;
    private Integer freeQuantity;
    private BigDecimal estimatedBonusValue;

    public void markInvalid(String message) {
        this.isValid = false;
        if (!messageBuilder.isEmpty()) {
            messageBuilder.append(" ");
        }
        messageBuilder.append(message);
    }
}
