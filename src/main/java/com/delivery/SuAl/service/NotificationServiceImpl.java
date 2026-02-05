package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Notification;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.NotificationMapper;
import com.delivery.SuAl.model.enums.ReceiverType;
import com.delivery.SuAl.model.request.notification.NotificationRequest;
import com.delivery.SuAl.model.response.notification.NotificationResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final PushNotificationService pushNotificationService;

    @Override
    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        Notification notification = notificationMapper.toEntity(request);

        Notification savedNotification = notificationRepository.save(notification);

        pushNotificationService.sendPushNotification(savedNotification.getId());

        return notificationMapper.toResponse(savedNotification);
    }

    @Override
    @Transactional
    public List<NotificationResponse> createNotificationsBatch(List<NotificationRequest> requests) {
        List<Notification> notifications = requests.stream()
                .map(notificationMapper::toEntity)
                .collect(Collectors.toList());

        List<Notification> savedNotifications = notificationRepository.saveAll(notifications);

        savedNotifications.forEach(notification ->
                pushNotificationService.sendPushNotification(notification.getId())
        );

        return notificationMapper.toResponseList(savedNotifications);
    }

    @Override
    public NotificationResponse getNotificationById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification with id " + id + " not found"));
        return notificationMapper.toResponse(notification);
    }

    @Override
    public List<NotificationResponse> getNotificationsByReceiver(ReceiverType receiverType, Long receiverId) {
        List<Notification> notifications = notificationRepository
                .findByReceiverTypeAndReceiverIdOrderByCreatedAtDesc(receiverType, receiverId);
        return notificationMapper.toResponseList(notifications);
    }

    @Override
    public List<NotificationResponse> getUnreadNotifications(ReceiverType receiverType, Long receiverId) {
        List<Notification> notifications = notificationRepository
                .findByReceiverTypeAndReceiverIdAndIsReadOrderByCreatedAtDesc(receiverType, receiverId, false);
        return notificationMapper.toResponseList(notifications);
    }

    @Override
    public PageResponse<NotificationResponse> getNotificationsByReceiverPaginated(
            ReceiverType receiverType, Long receiverId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository
                .findByReceiverTypeAndReceiverId(receiverType, receiverId, pageable);

        List<NotificationResponse> responses = notifications.getContent().stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(responses, notifications);

    }

    @Override
    public Long getUnreadCount(ReceiverType receiverType, Long receiverId) {
        return notificationRepository.countByReceiverTypeAndReceiverIdAndIsRead(
                receiverType, receiverId, false
        );
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification with id " + id + " not found"));
        notification.setIsRead(true);
        Notification updatedNotification = notificationRepository.save(notification);
        return notificationMapper.toResponse(updatedNotification);
    }

    @Override
    @Transactional
    public void markAllAsRead(ReceiverType receiverType, Long receiverId) {
        int updatedCount = notificationRepository.markAllAsReadBulk(receiverType, receiverId);
        log.info("Marked {} notifications as read for receiver: {} ({})",
                updatedCount, receiverId, receiverType);
    }

    @Override
    @Transactional
    public void deleteNotification(Long id) {
        if (!notificationRepository.existsById(id)) {
            throw new NotFoundException("Notification with id " + id + " not found");
        }
        notificationRepository.deleteById(id);
    }
}
