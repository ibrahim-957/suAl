package com.delivery.SuAl.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@Slf4j
public class SmsServiceImpl implements SmsService {
    @Value("${sms.login}")
    private String login;

    @Value("${sms.password}")
    private String password;

    @Value("${sms.sender}")
    private String sender;

    @Value("${sms.base-url}")
    private String baseUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public void sendOtp(String phoneNumber, String otpCode) {
        String msisdn = "+994" + normalizePhone(phoneNumber);
        String text = "Sizin təsdiq kodunuz: " + otpCode;
        try {
            String passwordHash = md5(password);
            String keySource = passwordHash + login + text + msisdn + sender;
            String key = md5(keySource);

            URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/send")
                    .queryParam("login", login)
                    .queryParam("msisdn", msisdn)
                    .queryParam("text", text)
                    .queryParam("sender", sender)
                    .queryParam("key", key)
                    .queryParam("unicode", 0)
                    .build()
                    .toUri();

            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(uri)
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("SMS sending failed for phone: {} — status: {}, body: {}",
                        phoneNumber, response.statusCode(), response.body());
                throw new RuntimeException("SMS could not be sent. Status: " + response.statusCode());
            }

            log.info("OTP SMS sent successfully to phone ending in: {}",
                    phoneNumber.substring(Math.max(0, phoneNumber.length() - 4)));

        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error while sending SMS to phone: {}", phoneNumber, ex);
            throw new RuntimeException("SMS sending failed unexpectedly", ex);
        }
    }

    private String normalizePhone(String phoneNumber) {
        if (phoneNumber.startsWith("+994")) {
            return phoneNumber.substring(4);
        }
        if (phoneNumber.startsWith("994")) {
            return phoneNumber.substring(3);
        }
        if (phoneNumber.startsWith("0")) {
            return phoneNumber.substring(1);
        }
        return phoneNumber;
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("MD5 algorithm not available", ex);
        }
    }
}
