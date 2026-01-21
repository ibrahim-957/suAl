package com.delivery.SuAl.exception;

public class OrderDeletionException extends RuntimeException {
    public OrderDeletionException(String message) {
        super(message);
    }
    public OrderDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}
