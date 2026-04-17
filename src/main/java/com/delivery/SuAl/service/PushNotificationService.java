package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.DeviceToken;
import com.delivery.SuAl.entity.Notification;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.exception.TransientPushException;
import com.delivery.SuAl.repository.DeviceTokenRepository;
import com.delivery.SuAl.repository.NotificationRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
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
    @Retryable(
            value = {FirebaseMessagingException.class, TransientPushException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            recover = "recoverPushNotification"
    )
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
                Message message = buildFirebaseMessage(notification, deviceToken);
                String response = FirebaseMessaging.getInstance().send(message);

                log.info("Successfully sent push notification to token {}. Response: {}",
                        maskToken(deviceToken.getFcmToken()), response);
                atLeastOneSent = true;

            } catch (FirebaseMessagingException ex) {
                if (isTokenInvalid(ex)) {
                    log.warn("Token is invalid or unregistered. Deactivating token: {}",
                            maskToken(deviceToken.getFcmToken()));
                    deviceToken.setIsActive(false);
                    deviceTokenRepository.save(deviceToken);
                } else {
                    log.error("Transient error sending push notification to token: {}. Will retry.",
                            maskToken(deviceToken.getFcmToken()), ex);
                    throw new TransientPushException("Retry needed", ex);
                }
            }
        }

        if (atLeastOneSent) {
            notification.setPushSent(true);
            notificationRepository.save(notification);
            log.info("Notification {} marked as sent", notificationId);
        }
    }

    @Recover
    public void recoverPushNotification(FirebaseMessagingException ex, Long notificationId) {
        log.error("Failed to send push notification {} after all retry attempts. Error: {}",
                notificationId, ex.getMessage());

        // Optional: Store in dead letter queue for manual review
        // deadLetterQueueService.add(notificationId, ex.getMessage());

        // Don't rethrow - we want to fail gracefully
        // The notification will remain with pushSent=false for potential later retry
    }

    private Message buildFirebaseMessage(Notification notification, DeviceToken deviceToken) {
        return Message.builder()
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
