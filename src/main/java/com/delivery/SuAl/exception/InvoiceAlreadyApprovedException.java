package com.delivery.SuAl.exception;

public class InvoiceAlreadyApprovedException extends RuntimeException {
    public InvoiceAlreadyApprovedException(String message) {
        super(message);
    }
}
