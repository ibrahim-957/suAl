package com.delivery.SuAl.model.enums;

public enum StockReservationType {
    NONE,           // No reservation
    SOFT,           // Reserved at PENDING - can be reclaimed if expires
    HARD            // Reserved at APPROVED - locked until completion
}
