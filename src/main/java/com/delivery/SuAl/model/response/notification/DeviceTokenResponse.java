package com.delivery.SuAl.model.response.notification;

import com.delivery.SuAl.model.enums.DeviceType;
import com.delivery.SuAl.model.enums.ReceiverType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeviceTokenResponse {
    private Long id;
    private ReceiverType receiverType;
    private Long receiverId;
    private String fcmToken;
    private DeviceType deviceType;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
