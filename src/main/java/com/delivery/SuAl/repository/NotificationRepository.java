package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Notification;
import com.delivery.SuAl.model.enums.ReceiverType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true " +
            "WHERE n.receiverType = :type AND n.receiverId = :id AND n.isRead = false")
    int markAllAsReadBulk(@Param("type") ReceiverType receiverType, @Param("id") Long receiverId);
}
