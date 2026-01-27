package com.delivery.SuAl.controller;

import com.delivery.SuAl.model.enums.ReceiverType;
import com.delivery.SuAl.model.request.notification.DeviceTokenRequest;
import com.delivery.SuAl.model.response.notification.DeviceTokenResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.service.DeviceTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @RequestBody DeviceTokenRequest request
            ){
        DeviceTokenResponse response = deviceTokenService.registerDeviceToken(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/receiver")
    public ResponseEntity<ApiResponse<List<DeviceTokenResponse>>> getActiveTokensByReceiver(
            @RequestParam ReceiverType receiverType,
            @RequestParam Long receiverId
            ){
        List<DeviceTokenResponse> responses = deviceTokenService.getActiveTokensByReceiver(receiverType, receiverId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(responses));
    }

    @PatchMapping("/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateDeviceToken(
            @RequestParam String fcmToken
    ){
        deviceTokenService.deactivateToken(fcmToken);
        return ResponseEntity
                .ok(ApiResponse.success(null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteToken(@RequestParam String fcmToken) {
        deviceTokenService.deleteToken(fcmToken);
        return ResponseEntity
                .ok(ApiResponse.success("Device token deleted successfully", null));
    }
}
