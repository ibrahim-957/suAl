package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Notification;
import com.delivery.SuAl.model.request.notification.NotificationRequest;
import com.delivery.SuAl.model.response.notification.NotificationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    Notification toEntity(NotificationRequest request);

    NotificationResponse toResponse(Notification notification);

    void updateEntityFromRequest(NotificationRequest request, @MappingTarget Notification notification);

    List<NotificationResponse> toResponseList(List<Notification> notifications);
}
