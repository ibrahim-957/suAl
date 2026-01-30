package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.DeviceToken;
import com.delivery.SuAl.mapper.DeviceTokenMapper;
import com.delivery.SuAl.model.enums.ReceiverType;
import com.delivery.SuAl.model.request.notification.DeviceTokenRequest;
import com.delivery.SuAl.model.response.notification.DeviceTokenResponse;
import com.delivery.SuAl.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceTokenServiceImpl implements DeviceTokenService{
    private final DeviceTokenRepository deviceTokenRepository;
    private final DeviceTokenMapper deviceTokenMapper;

    @Override
    @Transactional
    public DeviceTokenResponse registerDeviceToken(DeviceTokenRequest request) {
        Optional<DeviceToken> existingToken = deviceTokenRepository
                .findByReceiverTypeAndReceiverIdAndFcmToken(
                        request.getReceiverType(),
                        request.getReceiverId(),
                        request.getFcmToken()
                );

        if (existingToken.isPresent()) {
            DeviceToken token = existingToken.get();
            token.setIsActive(true);
            token.setDeviceType(request.getDeviceType());
            DeviceToken updated = deviceTokenRepository.save(token);
            return deviceTokenMapper.toResponse(updated);
        }

        DeviceToken deviceToken = deviceTokenMapper.toEntity(request);
        deviceToken.setIsActive(true);
        DeviceToken saved = deviceTokenRepository.save(deviceToken);
        return deviceTokenMapper.toResponse(saved);
    }

    @Override
    public List<DeviceTokenResponse> getActiveTokensByReceiver(
            ReceiverType receiverType, Long receiverId) {
        List<DeviceToken> tokens = deviceTokenRepository
                .findByReceiverTypeAndReceiverIdAndIsActiveTrue(receiverType, receiverId);
        return deviceTokenMapper.toResponseList(tokens);
    }

    @Override
    @Transactional
    public void deactivateToken(String fcmToken) {
        Optional<DeviceToken> token = deviceTokenRepository.findByFcmToken(fcmToken);
        token.ifPresent(deviceToken -> {
            deviceToken.setIsActive(false);
            deviceTokenRepository.save(deviceToken);
        });
    }

    @Override
    @Transactional
    public void deleteToken(String fcmToken) {
        deviceTokenRepository.deleteByFcmToken(fcmToken);
    }
}
