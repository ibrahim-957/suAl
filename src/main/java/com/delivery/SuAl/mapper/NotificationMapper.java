package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Notification;
import com.delivery.SuAl.model.request.notification.NotificationRequest;
import com.delivery.SuAl.model.response.notification.NotificationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {DateTimeMapper.class})
public interface NotificationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "isRead", constant = "false")
    @Mapping(target = "pushSent", constant = "false")
    Notification toEntity(NotificationRequest request);

    @Mapping(target = "createdAt", qualifiedByName = "utcToBaku")
    NotificationResponse toResponse(Notification notification);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "isRead", ignore = true)
    @Mapping(target = "pushSent", ignore = true)
    void updateEntityFromRequest(NotificationRequest request, @MappingTarget Notification notification);

    List<NotificationResponse> toResponseList(List<Notification> notifications);

}
