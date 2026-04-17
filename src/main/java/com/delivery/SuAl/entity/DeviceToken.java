package com.delivery.SuAl.entity;

import com.delivery.SuAl.model.enums.DeviceType;
import com.delivery.SuAl.model.enums.ReceiverType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "device_tokens",
        indexes = {
                @Index(name = "idx_device_receiver", columnList = "receiver_type, receiver_id"),
                @Index(name = "idx_device_token", columnList = "fcm_token")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeviceToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "receiver_id")
    private Long receiverId;

    @Column(name = "fcm_token", nullable = false, unique = true)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type",  nullable = false)
    private DeviceType deviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "receiver_type", nullable = false)
    private ReceiverType receiverType;


    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}