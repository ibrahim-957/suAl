package com.delivery.SuAl.controller;

import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.model.request.notification.DeviceTokenRequest;
import com.delivery.SuAl.model.response.notification.DeviceTokenResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.service.DeviceTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/api/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<DeviceTokenResponse>> registerDeviceToken(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody DeviceTokenRequest request
    ) {
        DeviceTokenResponse response = deviceTokenService.registerDeviceToken(user, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeviceTokenResponse>>> getMyActiveTokens(
            @AuthenticationPrincipal User user
    ) {
        List<DeviceTokenResponse> responses = deviceTokenService.getActiveTokensByReceiver(user);
        return ResponseEntity
                .ok(ApiResponse.success(responses));
    }

    @PatchMapping("/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateDeviceToken(
            @AuthenticationPrincipal User user,
            @RequestParam String fcmToken
    ) {
        deviceTokenService.deactivateToken(user, fcmToken);
        return ResponseEntity
                .ok(ApiResponse.success(null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteToken(
            @AuthenticationPrincipal User user,
            @RequestParam String fcmToken
    ) {
        deviceTokenService.deleteToken(user, fcmToken);
        return ResponseEntity
                .ok(ApiResponse.success("Device token deleted successfully", null));
    }
}