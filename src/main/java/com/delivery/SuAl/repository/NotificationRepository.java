package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Notification;
import com.delivery.SuAl.model.enums.ReceiverType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByReceiverTypeAndReceiverIdOrderByCreatedAtDesc(
            ReceiverType receiverType,
            Long receiverId
    );

    List<Notification> findByReceiverTypeAndReceiverIdAndIsReadOrderByCreatedAtDesc(
            ReceiverType receiverType,
            Long receiverId,
            Boolean isRead
    );

    Page<Notification> findByReceiverTypeAndReceiverId(
            ReceiverType receiverType,
            Long receiverId,
            Pageable pageable
    );

    Long countByReceiverTypeAndReceiverIdAndIsRead(
            ReceiverType receiverType,
            Long receiverId,
            Boolean isRead
    );

    List<Notification> findByPushSentFalse();
}
