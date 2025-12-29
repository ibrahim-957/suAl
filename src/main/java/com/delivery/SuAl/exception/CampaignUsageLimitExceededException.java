package com.delivery.SuAl.exception;

public class CampaignUsageLimitExceededException extends RuntimeException {
    public CampaignUsageLimitExceededException(String message) {
        super(message);
    }
}
