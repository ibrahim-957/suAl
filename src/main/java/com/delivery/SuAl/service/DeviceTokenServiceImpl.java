package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Admin;
import com.delivery.SuAl.entity.Customer;
import com.delivery.SuAl.entity.DeviceToken;
import com.delivery.SuAl.entity.Driver;
import com.delivery.SuAl.entity.Operator;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.DeviceTokenMapper;
import com.delivery.SuAl.model.enums.ReceiverType;
import com.delivery.SuAl.model.request.notification.DeviceTokenRequest;
import com.delivery.SuAl.model.response.notification.DeviceTokenResponse;
import com.delivery.SuAl.repository.AdminRepository;
import com.delivery.SuAl.repository.CustomerRepository;
import com.delivery.SuAl.repository.DeviceTokenRepository;
import com.delivery.SuAl.repository.DriverRepository;
import com.delivery.SuAl.repository.OperatorRepository;
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
    private final AdminRepository adminRepository;
    private final CustomerRepository customerRepository;
    private final OperatorRepository operatorRepository;
    private final DriverRepository driverRepository;

    @Override
    @Transactional
    public DeviceTokenResponse registerDeviceToken(User user, DeviceTokenRequest request) {
        Long receiverId = resolveRoleEntityId(user);
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
        deviceToken.setReceiverId(receiverId);
        deviceToken.setReceiverType(receiverType);
        DeviceToken saved = deviceTokenRepository.save(deviceToken);
        return deviceTokenMapper.toResponse(saved);
    }

    @Override
    public List<DeviceTokenResponse> getActiveTokensByReceiver(User user) {
        Long receiverId = resolveRoleEntityId(user);
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
        Long receiverId = resolveRoleEntityId(user);
        ReceiverType receiverType = ReceiverTypeResolver.resolve(user.getRole());

        if (!token.getReceiverId().equals(receiverId) ||
                !token.getReceiverType().equals(receiverType)) {
            throw new AccessDeniedException("You do not have permission to modify this device token");
        }
    }

    private Long resolveRoleEntityId(User user) {
        return switch (user.getRole()) {
            case ADMIN -> adminRepository.findByUserId(user.getId())
                    .map(Admin::getId)
                    .orElseThrow(() -> new NotFoundException("Admin not found for user: " + user.getId()));
            case CUSTOMER -> customerRepository.findByUserId(user.getId())
                    .map(Customer::getId)
                    .orElseThrow(() -> new NotFoundException("Customer not found for user: " + user.getId()));
            case OPERATOR -> operatorRepository.findByUserId(user.getId())
                    .map(Operator::getId)
                    .orElseThrow(() -> new NotFoundException("Operator not found for user: " + user.getId()));
            case DRIVER -> driverRepository.findByUserId(user.getId())
                    .map(Driver::getId)
                    .orElseThrow(() -> new NotFoundException("Driver not found for user: " + user.getId()));
            default -> throw new IllegalStateException(
                    "Unsupported role for device token: " + user.getRole());
        };
    }
}
