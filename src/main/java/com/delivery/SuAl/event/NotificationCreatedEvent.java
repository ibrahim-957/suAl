package com.delivery.SuAl.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationCreatedEvent {
    private final Long notificationId;
}
