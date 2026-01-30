package com.delivery.SuAl.model.response.notification;

import com.delivery.SuAl.model.enums.NotificationType;
import com.delivery.SuAl.model.enums.ReceiverType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private ReceiverType receiverType;
    private Long receiverId;
    private NotificationType notificationType;
    private String title;
    private String message;
    private Long referenceId;
    private Boolean isRead;
    private Boolean pushSent;
    private LocalDateTime createdAt;
}
