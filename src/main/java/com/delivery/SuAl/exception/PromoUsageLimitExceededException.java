package com.delivery.SuAl.exception;

public class PromoUsageLimitExceededException extends RuntimeException {
    public PromoUsageLimitExceededException(String message) {
        super(message);
    }
}
