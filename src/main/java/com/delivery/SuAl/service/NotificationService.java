package com.delivery.SuAl.service;

import com.delivery.SuAl.model.enums.ReceiverType;
import com.delivery.SuAl.model.request.notification.NotificationRequest;
import com.delivery.SuAl.model.response.notification.NotificationResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {
    NotificationResponse createNotification(NotificationRequest request);

    NotificationResponse getNotificationById(Long id);

    List<NotificationResponse> getNotificationsByReceiver(ReceiverType receiverType, Long receiverId);

    List<NotificationResponse> getUnreadNotifications(ReceiverType receiverType, Long receiverId);

    PageResponse<NotificationResponse> getNotificationsByReceiverPaginated(
            ReceiverType receiverType,
            Long receiverId,
            Pageable pageable
    );

    Long getUnreadCount(ReceiverType receiverType, Long receiverId);

    NotificationResponse markAsRead(Long id);

    void markAllAsRead(ReceiverType receiverType, Long receiverId);

    void deleteNotification(Long id);
}
