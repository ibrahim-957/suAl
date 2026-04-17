package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.OtpCode;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    private static final int OTP_EXPIRY_MINUTES = 2;
    private final OtpRepository otpRepository;
    private final SmsService smsService;

    @Override
    @Transactional
    public void sendOtp(String phoneNumber) {
        otpRepository.deleteByPhoneNumber(phoneNumber);

        String code = generateCode();

        OtpCode otpCode = OtpCode.builder()
                .phoneNumber(phoneNumber)
                .code(code)
                .isUsed(false)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(OTP_EXPIRY_MINUTES))
                .build();

        otpRepository.save(otpCode);

        smsService.sendOtp(phoneNumber, code);

        log.info("OTP sent to phone ending in: {}",
                phoneNumber.substring(Math.max(0, phoneNumber.length() - 4)));
    }

    @Override
    @Transactional
    public void verifyOtp(String phoneNumber, String code) {
        OtpCode otpCode = otpRepository.findByPhoneNumberAndIsUsedFalse(phoneNumber)
                .orElseThrow(() -> new NotFoundException("No active OTP found for this phone number"));

        if (otpCode.isUsed()) {
            otpRepository.delete(otpCode);
            throw new IllegalArgumentException("OTP has expired. Please request a new one");
        }

        if (!otpCode.getCode().equals(code)) {
            throw new IllegalArgumentException("Invalid OTP code");
        }

        otpCode.setUsed(true);
        otpRepository.save(otpCode);

        log.info("OTP verified successfully for phone ending in: {}",
                phoneNumber.substring(Math.max(0, phoneNumber.length() - 4)));
    }

    @Override
    @Scheduled(fixedRateString = "PT10M")
    @Transactional
    public void cleanupExpiredOtps() {
        log.debug("Running OTP cleanup job");
        otpRepository.deleteExpiredOtps(LocalDateTime.now(ZoneOffset.UTC));
    }

    private String generateCode(){
        int code = 100000 + new Random().nextInt(899999);
        return String.valueOf(code);
    }
}
