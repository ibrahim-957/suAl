package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.DeviceToken;
import com.delivery.SuAl.entity.Notification;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.repository.DeviceTokenRepository;
import com.delivery.SuAl.repository.NotificationRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRepository notificationRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendPushNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification Not Found: " + notificationId));

        List<DeviceToken> tokens = deviceTokenRepository
                .findByReceiverTypeAndReceiverIdAndIsActiveTrue(
                        notification.getReceiverType(),
                        notification.getReceiverId()
                );

        if (tokens.isEmpty()) {
            log.warn("No active device tokens found for receiver: {} with id: {}",
                    notification.getReceiverType(), notification.getReceiverId());
            return;
        }

        boolean atLeastOneSent = false;

        for (DeviceToken deviceToken : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(deviceToken.getFcmToken())
                        .setNotification(
                                com.google.firebase.messaging.Notification.builder()
                                        .setTitle(notification.getTitle())
                                        .setBody(notification.getMessage())
                                        .build()
                        )
                        .putData("notificationId", notification.getId().toString())
                        .putData("notificationType", notification.getNotificationType().name())
                        .putData("referenceId", notification.getReferenceId() != null ?
                                notification.getReferenceId().toString() : "")
                        .build();

                String response = FirebaseMessaging.getInstance().send(message);
                log.info("Successfully sent push notification. Response: {}", response);
                atLeastOneSent = true;
            } catch (FirebaseMessagingException ex) {
                log.error("Failed to send push notification to token: {}",
                        maskToken(deviceToken.getFcmToken()), ex);

                if (isTokenInvalid(ex)) {
                    log.warn("Deactivating invalid token: {}", maskToken(deviceToken.getFcmToken()));
                    deviceToken.setIsActive(false);
                    deviceTokenRepository.save(deviceToken);
                }
            }
        }

        if (atLeastOneSent) {
            notification.setPushSent(true);
            notificationRepository.save(notification);
        }
    }

    private boolean isTokenInvalid(FirebaseMessagingException e) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();
        return errorCode == MessagingErrorCode.UNREGISTERED ||
                errorCode == MessagingErrorCode.INVALID_ARGUMENT;
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
    }
}
