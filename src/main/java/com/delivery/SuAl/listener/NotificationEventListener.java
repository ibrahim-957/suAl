package com.delivery.SuAl.listener;

import com.delivery.SuAl.event.NotificationCreatedEvent;
import com.delivery.SuAl.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {
    private final PushNotificationService  pushNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        log.info("Transaction committed, sending push notification for ID: {}", event.getNotificationId());
        pushNotificationService.sendPushNotification(event.getNotificationId());
    }
}
