package com.delivery.SuAl.model.request.notification;

import com.delivery.SuAl.model.enums.DeviceType;
import com.delivery.SuAl.model.enums.ReceiverType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeviceTokenRequest {
    private ReceiverType receiverType;
    private Long receiverId;
    private String fcmToken;
    private DeviceType deviceType;
}
