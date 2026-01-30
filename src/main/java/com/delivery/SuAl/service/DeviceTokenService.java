package com.delivery.SuAl.service;

import com.delivery.SuAl.model.enums.ReceiverType;
import com.delivery.SuAl.model.request.notification.DeviceTokenRequest;
import com.delivery.SuAl.model.response.notification.DeviceTokenResponse;

import java.util.List;

public interface DeviceTokenService {
    DeviceTokenResponse registerDeviceToken(DeviceTokenRequest request);

    List<DeviceTokenResponse> getActiveTokensByReceiver(ReceiverType receiverType, Long receiverId);

    void deactivateToken(String fcmToken);

    void deleteToken(String fcmToken);
}
