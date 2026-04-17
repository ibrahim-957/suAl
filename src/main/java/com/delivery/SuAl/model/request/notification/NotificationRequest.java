package com.delivery.SuAl.model.request.notification;

import com.delivery.SuAl.model.enums.NotificationType;
import com.delivery.SuAl.model.enums.ReceiverType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationRequest {
    @NotNull
    private ReceiverType receiverType;

    @NotNull
    private Long receiverId;

    @NotNull
    private NotificationType notificationType;

    @NotBlank
    private String title;

    @NotBlank
    private String message;

    private Long referenceId;
}
