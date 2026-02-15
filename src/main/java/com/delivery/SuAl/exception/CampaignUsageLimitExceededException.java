package com.delivery.SuAl.exception;

public class CampaignUsageLimitExceededException extends RuntimeException {
    public CampaignUsageLimitExceededException(String message) {
        super(message);
    }
    public CampaignUsageLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
