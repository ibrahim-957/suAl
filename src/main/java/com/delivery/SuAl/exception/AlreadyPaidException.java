package com.delivery.SuAl.exception;

public class AlreadyPaidException extends RuntimeException {
    public AlreadyPaidException(String message) {
        super(message);
    }
}
