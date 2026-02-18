package com.delivery.SuAl.service;

public interface OtpService {
    void sendOtp(String phoneNumber);

    void verifyOtp(String phoneNumber, String code);

    void cleanupExpiredOtps();
}
