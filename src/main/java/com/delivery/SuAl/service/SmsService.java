package com.delivery.SuAl.service;

public interface SmsService {
    void sendOtp(String phoneNumber, String otpCode);
}
