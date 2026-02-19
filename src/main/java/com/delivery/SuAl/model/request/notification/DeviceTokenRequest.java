package com.delivery.SuAl.model.request.notification;

import com.delivery.SuAl.model.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeviceTokenRequest {
    @NotBlank(message = "FCM token is required")
    private String fcmToken;

    @NotNull(message = "Device type is required")
    private DeviceType deviceType;
}
