package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.DeviceToken;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.DeviceTokenMapper;
import com.delivery.SuAl.model.enums.ReceiverType;
import com.delivery.SuAl.model.request.notification.DeviceTokenRequest;
import com.delivery.SuAl.model.response.notification.DeviceTokenResponse;
import com.delivery.SuAl.repository.DeviceTokenRepository;
import com.delivery.SuAl.util.ReceiverTypeResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceTokenServiceImpl implements DeviceTokenService {
    private final DeviceTokenRepository deviceTokenRepository;
    private final DeviceTokenMapper deviceTokenMapper;

    @Override
    @Transactional
    public DeviceTokenResponse registerDeviceToken(User user, DeviceTokenRequest request) {
        Long receiverId = user.getTargetId();
        ReceiverType receiverType = ReceiverTypeResolver.resolve(user.getRole());

        Optional<DeviceToken> existingToken = deviceTokenRepository
                .findByReceiverTypeAndReceiverIdAndFcmToken(
                        receiverType,
                        receiverId,
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
    public List<DeviceTokenResponse> getActiveTokensByReceiver(User user) {
        Long receiverId = user.getTargetId();
        ReceiverType receiverType = ReceiverTypeResolver.resolve(user.getRole());

        List<DeviceToken> tokens = deviceTokenRepository
                .findByReceiverTypeAndReceiverIdAndIsActiveTrue(receiverType, receiverId);

        return deviceTokenMapper.toResponseList(tokens);
    }

    @Override
    @Transactional
    public void deactivateToken(User user, String fcmToken) {
        DeviceToken token = deviceTokenRepository.findByFcmToken(fcmToken)
                .orElseThrow(() -> new NotFoundException("Device token not found"));

        verifyOwnership(user, token);

        token.setIsActive(false);
        deviceTokenRepository.save(token);
    }

    @Override
    @Transactional
    public void deleteToken(User user, String fcmToken) {
        DeviceToken token = deviceTokenRepository.findByFcmToken(fcmToken)
                .orElseThrow(() -> new NotFoundException("Device token not found"));

        verifyOwnership(user, token);

        deviceTokenRepository.delete(token);
    }

    private void verifyOwnership(User user, DeviceToken token) {
        Long receiverId = user.getTargetId();
        ReceiverType receiverType = ReceiverTypeResolver.resolve(user.getRole());

        if (!token.getReceiverId().equals(receiverId) ||
                !token.getReceiverType().equals(receiverType)) {
            throw new AccessDeniedException("You do not have permission to modify this device token");
        }
    }
}
