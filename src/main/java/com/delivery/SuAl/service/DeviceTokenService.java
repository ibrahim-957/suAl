package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.model.request.notification.DeviceTokenRequest;
import com.delivery.SuAl.model.response.notification.DeviceTokenResponse;

import java.util.List;

public interface DeviceTokenService {
    DeviceTokenResponse registerDeviceToken(User user, DeviceTokenRequest request);

    List<DeviceTokenResponse> getActiveTokensByReceiver(User user);

    void deactivateToken(User user);

    void deleteToken(User user);
}
