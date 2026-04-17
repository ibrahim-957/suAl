package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.DeviceToken;
import com.delivery.SuAl.model.enums.ReceiverType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    List<DeviceToken> findByReceiverTypeAndReceiverIdAndIsActiveTrue(
            ReceiverType receiverType,
            Long receiverId
    );

    Optional<DeviceToken> findByFcmToken(String fcmToken);

    Optional<DeviceToken> findByReceiverTypeAndReceiverIdAndFcmToken(
            ReceiverType receiverType,
            Long receiverId,
            String fcmToken
    );

    void deleteByFcmToken(String fcmToken);

    List<DeviceToken> findAllByReceiverIdAndReceiverType(Long receiverId, ReceiverType receiverType);

}