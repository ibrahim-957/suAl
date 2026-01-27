package com.delivery.SuAl.model.request.notification;

import com.delivery.SuAl.model.enums.NotificationType;
import com.delivery.SuAl.model.enums.ReceiverType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationRequest {
    private ReceiverType receiverType;
    private Long receiverId;
    private NotificationType notificationType;
    private String title;
    private String message;
    private Long referenceId;
}
