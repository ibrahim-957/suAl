package com.delivery.SuAl.controller;

import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.model.request.auth.AuthenticationRequest;
import com.delivery.SuAl.model.request.auth.ChangePasswordRequest;
import com.delivery.SuAl.model.request.auth.RefreshTokenRequest;
import com.delivery.SuAl.model.request.auth.SendOtpRequest;
import com.delivery.SuAl.model.request.auth.VerifyOtpRequest;
import com.delivery.SuAl.model.response.auth.AuthenticationResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.service.AuthenticationService;
import com.delivery.SuAl.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/auth")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AuthController {
    private final AuthenticationService authenticationService;
    private final OtpService otpService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(authenticationService.refreshToken(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        log.info("Password change request from user: {}", user.getUsername());

        ApiResponse<String> response = authenticationService.changePassword(request, user.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/otp/send")
    public ResponseEntity<ApiResponse<String>> sendOtp(
            @Valid @RequestBody SendOtpRequest request) {
        otpService.sendOtp(request.getPhoneNumber());

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("OTP Sent Successfully")
                .data("Please check your SMS")
                .build());
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<AuthenticationResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        otpService.verifyOtp(request.getPhoneNumber(), request.getCode());

        AuthenticationResponse response = authenticationService
                .authenticateCustomerAfterOtp(request.getPhoneNumber());

        return ResponseEntity.ok(response);
    }
}
